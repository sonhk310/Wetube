package com.wetube.wetube_service.service.video;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
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
import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.enumeration.PlaylistType;
import com.wetube.wetube_service.mapper.video.VideoMapper;
import com.wetube.wetube_service.repository.PlaylistVideoRepository;
import com.wetube.wetube_service.repository.channel.SubscriptionRepository;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.security.VideoAccess;
import com.wetube.wetube_service.service.video.HomeFeedService.Chip;
import com.wetube.wetube_service.service.video.HomeFeedService.Kind;

class HomeFeedServiceTest {

    private final RecService recService = mock(RecService.class);
    private final VideoRepository videoRepository = mock(VideoRepository.class);
    private final SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
    private final PlaylistVideoRepository playlistVideoRepository = mock(PlaylistVideoRepository.class);
    private final VideoMapper videoMapper = mock(VideoMapper.class);
    private final HomeFeedService service = new HomeFeedService(recService, videoRepository, subscriptionRepository,
            playlistVideoRepository, videoMapper);

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void defaults() {
        when(playlistVideoRepository.existsByPlaylist_User_IdAndPlaylist_PlaylistType(userId, PlaylistType.HISTORY)).thenReturn(true);
        when(subscriptionRepository.findSubscribedChannelIds(userId)).thenReturn(List.of());
        when(videoRepository.findRecentListed(any(), any(), any(), any())).thenReturn(Page.empty());
        when(recService.hasUnwatched(any(), any())).thenReturn(false);
        when(playlistVideoRepository.findByPlaylist_User_IdAndPlaylist_PlaylistTypeAndVideo_VideosStatusIn(any(), any(), any(), any()))
                .thenReturn(Page.empty());
    }

    private static List<String> ids(List<Chip> chips) {
        return chips.stream().map(Chip::id).toList();
    }

    @Test
    void noHistoryMeansNoChipsAtAll() {
        when(playlistVideoRepository.existsByPlaylist_User_IdAndPlaylist_PlaylistType(userId, PlaylistType.HISTORY)).thenReturn(false);

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

        List<Chip> chips = service.chips(userId, "s");

        // Không còn chip chủ đề lấy từ thẻ
        assertThat(ids(chips)).containsExactly("SUBSCRIPTIONS", "NEW", "UNWATCHED", "WATCHED");
        assertThat(chips.get(0).label()).isEqualTo("Kênh đăng ký");
    }

    @Test
    void subscriptionsChipHiddenWhenTheChannelsHaveNoVideos() {
        when(subscriptionRepository.findSubscribedChannelIds(userId)).thenReturn(List.of(UUID.randomUUID()));
        when(videoRepository.findFeed(any(), any(), any())).thenReturn(Page.empty());

        assertThat(ids(service.chips(userId, "s"))).doesNotContain("SUBSCRIPTIONS");
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
