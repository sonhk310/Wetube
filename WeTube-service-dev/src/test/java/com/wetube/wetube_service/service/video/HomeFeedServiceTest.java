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
import com.wetube.wetube_service.entity.video.UserTopic;
import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.enumeration.PlaylistType;
import com.wetube.wetube_service.enumeration.LiveStatus;
import com.wetube.wetube_service.mapper.video.VideoMapper;
import com.wetube.wetube_service.repository.PlaylistVideoRepository;
import com.wetube.wetube_service.repository.channel.SubscriptionRepository;
import com.wetube.wetube_service.repository.live.LiveStreamRepository;
import com.wetube.wetube_service.repository.video.UserTopicRepository;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.security.VideoAccess;
import com.wetube.wetube_service.service.video.HomeFeedService.Chip;
import com.wetube.wetube_service.service.video.HomeFeedService.Kind;

class HomeFeedServiceTest {

    private final RecService recService = mock(RecService.class);
    private final VideoRepository videoRepository = mock(VideoRepository.class);
    private final SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
    private final PlaylistVideoRepository playlistVideoRepository = mock(PlaylistVideoRepository.class);
    private final UserTopicRepository userTopicRepository = mock(UserTopicRepository.class);
    private final VideoMapper videoMapper = mock(VideoMapper.class);
    private final LiveStreamRepository liveStreamRepository = mock(LiveStreamRepository.class);
    private final HomeFeedService service = new HomeFeedService(recService, videoRepository, subscriptionRepository,
            playlistVideoRepository, userTopicRepository, new TopicCatalog(), videoMapper, liveStreamRepository);

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void defaults() {
        when(playlistVideoRepository.existsByPlaylist_User_IdAndPlaylist_PlaylistType(userId, PlaylistType.HISTORY)).thenReturn(true);
        when(subscriptionRepository.findSubscribedChannelIds(userId)).thenReturn(List.of());
        when(videoRepository.findRecentListed(any(), any(), any(), any())).thenReturn(Page.empty());
        when(recService.hasUnwatched(any(), any())).thenReturn(false);
        when(playlistVideoRepository.findByPlaylist_User_IdAndPlaylist_PlaylistTypeAndVideo_VideosStatusIn(any(), any(), any(), any()))
                .thenReturn(Page.empty());
        when(videoRepository.countListedByTopic(any(), any())).thenReturn(List.of());
        when(userTopicRepository.findAllByUserIdOrderByPointDesc(userId)).thenReturn(List.of());
    }

    /** Số video công khai theo chủ đề: "id", số, "id", số... */
    private void topicCounts(Object... idAndCount) {
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < idAndCount.length; i += 2) {
            rows.add(new Object[] {idAndCount[i], ((Integer) idAndCount[i + 1]).longValue()});
        }
        when(videoRepository.countListedByTopic(VideoAccess.LISTED, userId)).thenReturn(rows);
    }

    private List<String> topicChips() {
        return ids(service.chips(userId, "s")).stream().filter(id -> id.startsWith("topic:")).toList();
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
    void liveChipSitsAfterSubscriptionsAndOnlyWhenSomeoneIsLive() {
        UUID channel = UUID.randomUUID();
        when(subscriptionRepository.findSubscribedChannelIds(userId)).thenReturn(List.of(channel));
        when(videoRepository.findFeed(any(), any(), any())).thenReturn(new PageImpl<>(List.of(new Video())));
        when(videoRepository.findRecentListed(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(new Video())));

        assertThat(ids(service.chips(userId, "s"))).containsExactly("SUBSCRIPTIONS", "NEW");

        when(liveStreamRepository.existsByStatus(LiveStatus.LIVE)).thenReturn(true);
        List<Chip> chips = service.chips(userId, "s");

        assertThat(ids(chips)).containsExactly("SUBSCRIPTIONS", "LIVE", "NEW");
        assertThat(chips.get(1).label()).isEqualTo("Trực tiếp");
    }

    @Test
    void liveChipIsTheFirstOneWhenThereAreNoSubscriptionVideos() {
        when(liveStreamRepository.existsByStatus(LiveStatus.LIVE)).thenReturn(true);

        assertThat(ids(service.chips(userId, "s"))).containsExactly("LIVE");
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
    void topicChipsComeAfterTheFixedOnesWithStandardNames() {
        topicCounts("sports.bong-da", 2, "music", 1);

        List<Chip> chips = service.chips(userId, "s");

        // Bóng đá: chip "Thể thao" bị bỏ vì ra đúng cùng các video với "Bóng đá"
        assertThat(ids(chips)).containsExactly("topic:sports.bong-da", "topic:music");
        assertThat(chips).extracting(Chip::label).containsExactly("Bóng đá", "Âm nhạc");
    }

    @Test
    void aGroupWithSeveralSubTopicsGetsItsOwnChipCountingThemAll() {
        topicCounts("music.lo-fi", 1, "music.rock", 1, "sports", 1);

        assertThat(topicChips()).containsExactly("topic:music", "topic:music.lo-fi", "topic:music.rock", "topic:sports");
    }

    @Test
    void theViewersFavouriteTopicsComeFirstAndAtMostEight() {
        topicCounts("music", 1, "sports", 1, "gaming", 1, "food", 1, "travel", 1, "news", 1, "tech", 1, "art", 1,
                "books", 1, "podcast", 5);
        when(userTopicRepository.findAllByUserIdOrderByPointDesc(userId)).thenReturn(List.of(
                UserTopic.builder().userId(userId).topicId("food").point(50).build(),
                UserTopic.builder().userId(userId).topicId("khong-con-video").point(40).build()));

        List<String> topics = topicChips();

        assertThat(topics).hasSize(HomeFeedService.MAX_TOPIC_CHIPS);
        assertThat(topics.subList(0, 2)).containsExactly("topic:food", "topic:podcast");
    }

    @Test
    void topicFeedMatchesTheTopicAndItsSubTopics() {
        when(videoRepository.findListedByTopic(any(), any(), any(), any(), any())).thenReturn(Page.empty());

        service.topicFeed(userId, "music", PageRequest.of(0, 18));

        verify(videoRepository).findListedByTopic(VideoAccess.LISTED, "music", "music.%", userId, PageRequest.of(0, 18));
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
