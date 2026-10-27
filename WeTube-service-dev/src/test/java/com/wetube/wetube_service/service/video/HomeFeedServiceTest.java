package com.wetube.wetube_service.service.video;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.wetube.wetube_service.entity.playlist.PlaylistVideo;
import com.wetube.wetube_service.entity.video.Tag;
import com.wetube.wetube_service.entity.video.UserTag;
import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.enumeration.PlaylistType;
import com.wetube.wetube_service.mapper.video.VideoMapper;
import com.wetube.wetube_service.repository.PlaylistVideoRepository;
import com.wetube.wetube_service.repository.channel.SubscriptionRepository;
import com.wetube.wetube_service.repository.video.TagRepository;
import com.wetube.wetube_service.repository.video.UserTagRepository;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.security.VideoAccess;
import com.wetube.wetube_service.service.video.HomeFeedService.Chip;
import com.wetube.wetube_service.service.video.HomeFeedService.Kind;

class HomeFeedServiceTest {

    private final RecService recService = mock(RecService.class);
    private final VideoRepository videoRepository = mock(VideoRepository.class);
    private final SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
    private final PlaylistVideoRepository playlistVideoRepository = mock(PlaylistVideoRepository.class);
    private final UserTagRepository userTagRepository = mock(UserTagRepository.class);
    private final TagRepository tagRepository = mock(TagRepository.class);
    private final VideoMapper videoMapper = mock(VideoMapper.class);
    private final HomeFeedService service = new HomeFeedService(recService, videoRepository, subscriptionRepository,
            playlistVideoRepository, userTagRepository, tagRepository, videoMapper);

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void defaults() {
        when(playlistVideoRepository.existsByPlaylist_User_IdAndPlaylist_PlaylistType(userId, PlaylistType.HISTORY)).thenReturn(true);
        when(subscriptionRepository.findSubscribedChannelIds(userId)).thenReturn(List.of());
        when(videoRepository.findRecentListed(any(), any(), any(), any())).thenReturn(Page.empty());
        when(recService.hasUnwatched(any(), any())).thenReturn(false);
        when(playlistVideoRepository.findByPlaylist_User_IdAndPlaylist_PlaylistTypeAndVideo_VideosStatusIn(any(), any(), any(), any()))
                .thenReturn(Page.empty());
        when(tagRepository.findPopularTagNames(any(), any())).thenReturn(List.of());
        when(userTagRepository.findAllByUserIdOrderByPointDesc(userId)).thenReturn(List.of());
    }

    private static List<Object[]> popular(String... names) {
        List<Object[]> rows = new ArrayList<>();
        for (String n : names) {
            rows.add(new Object[] {n, 1L});
        }
        return rows;
    }

    private static List<String> ids(List<Chip> chips) {
        return chips.stream().map(Chip::id).toList();
    }

    @Test
    void noHistoryMeansNoChipsAtAll() {
        when(playlistVideoRepository.existsByPlaylist_User_IdAndPlaylist_PlaylistType(userId, PlaylistType.HISTORY)).thenReturn(false);
        when(tagRepository.findPopularTagNames(any(), any())).thenReturn(popular("anime"));

        assertThat(service.chips(userId, "s")).isEmpty();
    }

    @Test
    void emptyChipsAreHidden() {
        assertThat(service.chips(userId, "s")).isEmpty();
    }

    @Test
    void chipsComeInOrderAndOnlyThoseWithVideos() {
        UUID channel = UUID.randomUUID();
        when(subscriptionRepository.findSubscribedChannelIds(userId)).thenReturn(List.of(channel));
        when(videoRepository.findFeed(any(), any(), any())).thenReturn(new PageImpl<>(List.of(new Video())));
        when(videoRepository.findRecentListed(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(new Video())));
        when(recService.hasUnwatched(userId, "s")).thenReturn(true);
        when(playlistVideoRepository.findByPlaylist_User_IdAndPlaylist_PlaylistTypeAndVideo_VideosStatusIn(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(new PlaylistVideo())));
        when(tagRepository.findPopularTagNames(any(), any())).thenReturn(popular("anime", "nhac"));

        List<Chip> chips = service.chips(userId, "s");

        assertThat(ids(chips)).containsExactly("SUBSCRIPTIONS", "NEW", "UNWATCHED", "WATCHED", "tag:anime", "tag:nhac");
        assertThat(chips.get(0).label()).isEqualTo("Kênh đăng ký");
        assertThat(chips.get(4).label()).isEqualTo("#anime");
    }

    @Test
    void subscriptionsChipHiddenWhenTheChannelsHaveNoVideos() {
        when(subscriptionRepository.findSubscribedChannelIds(userId)).thenReturn(List.of(UUID.randomUUID()));
        when(videoRepository.findFeed(any(), any(), any())).thenReturn(Page.empty());

        assertThat(ids(service.chips(userId, "s"))).doesNotContain("SUBSCRIPTIONS");
    }

    @Test
    void theViewersFavouriteTagsComeFirstAndAtMostEightTopicChips() {
        UUID favouriteId = UUID.randomUUID();
        Tag favourite = new Tag();
        favourite.setName("nhac");
        when(tagRepository.findPopularTagNames(any(), any()))
                .thenReturn(popular("t1", "t2", "t3", "nhac", "t5", "t6", "t7", "t8", "t9", "t10"));
        when(userTagRepository.findAllByUserIdOrderByPointDesc(userId))
                .thenReturn(List.of(UserTag.builder().userId(userId).tagId(favouriteId).point(50).build()));
        when(tagRepository.findById(favouriteId)).thenReturn(Optional.of(favourite));

        List<String> topics = ids(service.chips(userId, "s"));

        assertThat(topics).hasSize(HomeFeedService.MAX_TOPIC_CHIPS);
        assertThat(topics.get(0)).isEqualTo("tag:nhac");
    }

    @Test
    void aFavouriteTagWithoutPublicVideosGetsNoChip() {
        UUID goneId = UUID.randomUUID();
        Tag gone = new Tag();
        gone.setName("khong-con-video");
        when(tagRepository.findPopularTagNames(any(), any())).thenReturn(popular("anime"));
        when(userTagRepository.findAllByUserIdOrderByPointDesc(userId))
                .thenReturn(List.of(UserTag.builder().userId(userId).tagId(goneId).point(50).build()));
        when(tagRepository.findById(goneId)).thenReturn(Optional.of(gone));

        assertThat(ids(service.chips(userId, "s"))).containsExactly("tag:anime");
    }

    @Test
    void newUploadsLookBackSevenDaysAndSkipTheViewersOwnVideos() {
        service.feed(userId, Kind.NEW, PageRequest.of(0, 18), "s");

        ArgumentCaptor<LocalDateTime> since = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(videoRepository).findRecentListed(eq(VideoAccess.LISTED), since.capture(), eq(userId), any());
        assertThat(since.getValue()).isBetween(LocalDateTime.now().minusDays(7).minusMinutes(1),
                LocalDateTime.now().minusDays(7).plusMinutes(1));
    }

    @Test
    void watchedShowsTheMostRecentlyWatchedFirst() {
        service.feed(userId, Kind.WATCHED, PageRequest.of(2, 18), "s");

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(playlistVideoRepository).findByPlaylist_User_IdAndPlaylist_PlaylistTypeAndVideo_VideosStatusIn(
                eq(userId), eq(PlaylistType.HISTORY), eq(VideoAccess.LISTED), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Order.desc("updatedAt")));
    }

    @Test
    void unwatchedDelegatesToTheRecommender() {
        service.feed(userId, Kind.UNWATCHED, PageRequest.of(0, 18), "s");

        verify(recService).unwatchedVideos(userId, PageRequest.of(0, 18), "s");
    }

    @Test
    void subscriptionsWithoutChannelsIsAnEmptyPage() {
        assertThat(service.feed(userId, Kind.SUBSCRIPTIONS, PageRequest.of(0, 18), "s").getContent()).isEmpty();
    }
}
