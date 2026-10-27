package com.wetube.wetube_service.service.video.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.test.util.ReflectionTestUtils;

import com.wetube.wetube_service.dto.video.RecommendVideoDto;
import com.wetube.wetube_service.enumeration.PlaylistType;
import com.wetube.wetube_service.mapper.video.VideoMapper;
import com.wetube.wetube_service.repository.PlaylistVideoRepository;
import com.wetube.wetube_service.repository.channel.SubscriptionRepository;
import com.wetube.wetube_service.repository.video.TagRepository;
import com.wetube.wetube_service.repository.video.UserTagRepository;
import com.wetube.wetube_service.search.SearchIndexes;
import com.wetube.wetube_service.search.VideoDocument;
import com.wetube.wetube_service.service.RedisService;
import com.wetube.wetube_service.service.video.WatchProgressService;

/** Gợi ý trang chủ: seed, cache, loại video của chính mình, hạ video đã xem hết, chip "Chưa xem". */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecServiceImplTest {

    private static final IndexCoordinates V2 = IndexCoordinates.of("videos_v2");

    @Mock UserTagRepository userTagRepo;
    @Mock TagRepository tagRepo;
    @Mock ElasticsearchOperations elasticOps;
    @Spy VideoMapper videoMapper = new com.wetube.wetube_service.mapper.video.VideoMapperImpl();
    @Mock RedisService redisService;
    @Mock SearchIndexes indexes;
    @Mock PlaylistVideoRepository playlistVideoRepo;
    @Mock SubscriptionRepository subscriptionRepo;
    @Mock WatchProgressService watchProgressService;

    @InjectMocks RecServiceImpl service;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "poolSize", 20);
        when(indexes.videos()).thenReturn(V2);
        when(userTagRepo.findAllByUserIdOrderByPointDesc(userId)).thenReturn(List.of());
        when(watchProgressService.getMany(any(), any())).thenReturn(Map.of());
        when(playlistVideoRepo.existsByPlaylist_User_IdAndPlaylist_PlaylistType(userId, PlaylistType.HISTORY)).thenReturn(true);
    }

    @SuppressWarnings("unchecked")
    private static SearchHits<VideoDocument> hitsOf(VideoDocument... docs) {
        SearchHits<VideoDocument> hits = mock(SearchHits.class);
        List<SearchHit<VideoDocument>> list = new ArrayList<>();
        for (VideoDocument d : docs) {
            SearchHit<VideoDocument> hit = mock(SearchHit.class);
            when(hit.getContent()).thenReturn(d);
            list.add(hit);
        }
        when(hits.getSearchHits()).thenReturn(list);
        when(hits.isEmpty()).thenReturn(list.isEmpty());
        return hits;
    }

    private static VideoDocument doc(UUID id, UUID owner) {
        VideoDocument d = new VideoDocument();
        d.setId(id.toString());
        d.setTitle("t");
        d.setUserId(owner.toString());
        d.setChannelId(UUID.randomUUID().toString());
        d.setCreatedAt(Instant.now());
        d.setTotalView(5);
        d.setDuration(100f);
        return d;
    }

    private void everySearchReturns(SearchHits<VideoDocument> hits) {
        when(elasticOps.search(any(Query.class), eq(VideoDocument.class), eq(V2))).thenReturn(hits);
    }

    @Test
    void cleanSeedAcceptsShortSafeStringsOnly() {
        assertThat(RecServiceImpl.cleanSeed("ab-12_Z")).isEqualTo("ab-12_Z");
        assertThat(RecServiceImpl.cleanSeed(null)).isEmpty();
        assertThat(RecServiceImpl.cleanSeed("a:b")).isEmpty();
        assertThat(RecServiceImpl.cleanSeed("x".repeat(33))).isEmpty();
    }

    @Test
    void theListIsCachedPerUserVersionAndSeed() {
        UUID video = UUID.randomUUID();
        everySearchReturns(hitsOf(doc(video, UUID.randomUUID())));
        when(redisService.get(any())).thenReturn(null);

        service.recommendVideos(userId, PageRequest.of(0, 10), "abc");

        verify(redisService).set(eq("rec:" + userId + ":0:abc"), eq(video.toString()), any());
    }

    @Test
    void anEvictedUserGetsANewCacheKeyForEverySeed() {
        when(redisService.get("rec:ver:" + userId)).thenReturn("12345");
        when(redisService.get(eq("rec:" + userId + ":12345:abc"))).thenReturn("");

        assertThat(service.recommendVideos(userId, PageRequest.of(0, 10), "abc").getContent()).isEmpty();

        service.evictRecommendations(userId);
        verify(redisService).set(eq("rec:ver:" + userId), anyString(), any());
    }

    @Test
    void ownVideosAreNeverRecommended() {
        UUID mine = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        everySearchReturns(hitsOf(doc(mine, userId), doc(other, UUID.randomUUID())));
        when(redisService.get(any())).thenReturn(null);

        service.recommendVideos(userId, PageRequest.of(0, 10), "");

        ArgumentCaptor<String> cached = ArgumentCaptor.forClass(String.class);
        verify(redisService).set(anyString(), cached.capture(), any());
        assertThat(cached.getValue()).isEqualTo(other.toString());
    }

    @Test
    void aVideoWatchedToTheEndSinksBelowOthers() {
        UUID seen = UUID.randomUUID();
        UUID fresh = UUID.randomUUID();
        everySearchReturns(hitsOf(doc(seen, UUID.randomUUID()), doc(fresh, UUID.randomUUID())));
        when(redisService.get(any())).thenReturn(null);
        when(watchProgressService.getMany(any(), any())).thenReturn(Map.of(seen, 95f));

        service.recommendVideos(userId, PageRequest.of(0, 10), "");

        ArgumentCaptor<String> cached = ArgumentCaptor.forClass(String.class);
        verify(redisService).set(anyString(), cached.capture(), any());
        assertThat(cached.getValue()).isEqualTo(fresh + "," + seen);
    }

    @Test
    void unwatchedDropsEveryVideoInTheWatchHistory() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        when(redisService.get(any())).thenReturn(a + "," + b + "," + c);
        when(playlistVideoRepo.findHistoryVideoIds(userId)).thenReturn(List.of(b));
        everySearchReturns(hitsOf(doc(a, UUID.randomUUID()), doc(c, UUID.randomUUID())));

        Page<RecommendVideoDto> page = service.unwatchedVideos(userId, PageRequest.of(0, 10), "");

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(RecommendVideoDto::getId).containsExactly(a, c);
        assertThat(service.hasUnwatched(userId, "")).isTrue();
    }

    @Test
    void nothingUnwatchedWhenEverythingIsInTheHistory() {
        UUID a = UUID.randomUUID();
        when(redisService.get(any())).thenReturn(a.toString());
        when(playlistVideoRepo.findHistoryVideoIds(userId)).thenReturn(List.of(a));

        assertThat(service.hasUnwatched(userId, "")).isFalse();
    }
}
