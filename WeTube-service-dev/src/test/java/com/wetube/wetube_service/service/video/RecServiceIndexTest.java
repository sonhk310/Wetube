package com.wetube.wetube_service.service.video;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import com.wetube.wetube_service.mapper.video.VideoMapper;
import com.wetube.wetube_service.repository.video.TagRepository;
import com.wetube.wetube_service.repository.video.UserTagRepository;
import com.wetube.wetube_service.search.SearchIndexes;
import com.wetube.wetube_service.search.VideoDocument;
import com.wetube.wetube_service.service.RedisService;
import com.wetube.wetube_service.service.video.impl.RecServiceImpl;

/**
 * Trang chủ phải đọc chỉ mục video đang dùng (videos_v2). Không nêu tên chỉ mục thì Spring Data lấy chỉ mục cũ
 * "videos" ghi trên VideoDocument: video mới không có trong đó và video đã ẩn vẫn còn, card thì mang tên / ảnh cũ.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecServiceIndexTest {

    private static final IndexCoordinates V2 = IndexCoordinates.of("videos_v2");

    @Mock UserTagRepository userTagRepo;
    @Mock TagRepository tagRepo;
    @Mock ElasticsearchOperations elasticOps;
    @Spy VideoMapper videoMapper = new com.wetube.wetube_service.mapper.video.VideoMapperImpl();
    @Mock RedisService redisService;
    @Mock SearchIndexes indexes;

    @InjectMocks RecServiceImpl service;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "poolSize", 20);
        when(indexes.videos()).thenReturn(V2);
        when(userTagRepo.findAllByUserIdOrderByPointDesc(userId)).thenReturn(List.of());
    }

    @SuppressWarnings("unchecked")
    private static SearchHits<VideoDocument> hitsOf(VideoDocument... docs) {
        SearchHits<VideoDocument> hits = mock(SearchHits.class);
        List<SearchHit<VideoDocument>> list = new java.util.ArrayList<>();
        for (VideoDocument d : docs) {
            SearchHit<VideoDocument> hit = mock(SearchHit.class);
            when(hit.getContent()).thenReturn(d);
            list.add(hit);
        }
        when(hits.getSearchHits()).thenReturn(list);
        when(hits.isEmpty()).thenReturn(list.isEmpty());
        return hits;
    }

    private static VideoDocument doc(UUID id, String picture) {
        VideoDocument d = new VideoDocument();
        d.setId(id.toString());
        d.setTitle("t");
        d.setName("Kênh");
        d.setPicture(picture);
        d.setCreatedAt(Instant.now());
        return d;
    }

    @Test
    void newUsersGetTheLatestVideosFromTheCurrentIndex() {
        UUID video = UUID.randomUUID();
        SearchHits<VideoDocument> hits = hitsOf(doc(video, "new.jpg"));
        when(redisService.get(any())).thenReturn(null);
        when(elasticOps.search(any(Query.class), eq(VideoDocument.class), eq(V2))).thenReturn(hits);

        service.recommendVideos(userId, PageRequest.of(0, 10));

        // Hai lần tìm (lấy danh sách id mới nhất, rồi lấy từng video) đều phải vào chỉ mục hiện tại
        verify(elasticOps, times(2)).search(any(Query.class), eq(VideoDocument.class), eq(V2));
        // Không được dùng hàm không nêu chỉ mục (sẽ rơi vào chỉ mục cũ "videos")
        verify(elasticOps, never()).search(any(Query.class), eq(VideoDocument.class));
    }

    @Test
    void cardsAreBuiltFromTheCurrentIndexDocuments() {
        UUID video = UUID.randomUUID();
        SearchHits<VideoDocument> hits = hitsOf(doc(video, "https://res.cloudinary.com/demo/image/upload/v2/channels/b.jpg"));
        when(redisService.get(any())).thenReturn(video.toString());
        when(elasticOps.search(any(Query.class), eq(VideoDocument.class), eq(V2))).thenReturn(hits);

        Page<RecommendVideoDto> page = service.recommendVideos(userId, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getPicture()).endsWith("channels/b.jpg");
        verify(elasticOps, never()).search(any(Query.class), eq(VideoDocument.class));
    }
}
