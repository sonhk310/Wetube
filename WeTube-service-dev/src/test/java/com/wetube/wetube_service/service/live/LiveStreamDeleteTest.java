package com.wetube.wetube_service.service.live;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.wetube.wetube_service.dto.LiveRecordingProperties;
import com.wetube.wetube_service.dto.live.ChannelLivesResponse;
import com.wetube.wetube_service.dto.live.DeleteLivesResponse;
import com.wetube.wetube_service.dto.live.MyPastLivesResponse;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.channel.Channel;
import com.wetube.wetube_service.entity.live.LiveStream;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.enumeration.LiveSource;
import com.wetube.wetube_service.enumeration.LiveStatus;
import com.wetube.wetube_service.enumeration.RecordingStatus;
import com.wetube.wetube_service.repository.UserRepository;
import com.wetube.wetube_service.repository.channel.ChannelRepository;
import com.wetube.wetube_service.repository.live.LiveStreamRepository;
import com.wetube.wetube_service.security.RateLimiter;
import com.wetube.wetube_service.security.RateLimiter.Limit;
import com.wetube.wetube_service.security.VideoAccess;
import com.wetube.wetube_service.service.CloudinaryService;
import com.wetube.wetube_service.service.notification.NotificationService;
import com.wetube.wetube_service.service.video.TopicCatalog;
import com.wetube.wetube_service.service.video.VideoService;

/** Quản lý các buổi đã phát ở Studio: danh sách, xoá nhiều buổi (kèm hoặc không kèm video xem lại), tab công khai. */
class LiveStreamDeleteTest {

    private static final String THUMB = "https://res.cloudinary.com/x/image/upload/t.jpg";
    private static final String BANNER = "https://res.cloudinary.com/x/image/upload/channels/banner.jpg";

    private final LiveStreamRepository repo = mock(LiveStreamRepository.class);
    private final UserRepository userRepo = mock(UserRepository.class);
    private final ChannelRepository channelRepo = mock(ChannelRepository.class);
    private final RateLimiter rateLimiter = mock(RateLimiter.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final LivePollService polls = mock(LivePollService.class);
    private final CloudinaryService cloudinary = mock(CloudinaryService.class);
    private final VideoService videos = mock(VideoService.class);
    private final TrackingTransactionManager tm = new TrackingTransactionManager();

    private final UUID owner = UUID.randomUUID();
    private final UUID channelId = UUID.randomUUID();
    private final List<LiveStream> all = new ArrayList<>();
    private LiveStreamService service;

    @BeforeEach
    void setUp() {
        service = new LiveStreamService(repo, userRepo, channelRepo, mock(LiveKitGateway.class), rateLimiter,
                mock(LiveEventPublisher.class), notifications, polls, cloudinary, new LiveRecordingProperties(),
                mock(TopicCatalog.class), videos, List.of(), tm);
        Channel channel = Channel.builder().id(channelId).name("Kênh Sơn").status(ActiveStatus.ACTIVE)
                .backgroundImgUrl(BANNER).build();
        when(userRepo.findById(owner)).thenReturn(Optional.of(
                AppUser.builder().id(owner).email("son@example.com").name("Sơn").channel(channel).build()));
        when(repo.findAllById(anyList())).thenAnswer(inv -> {
            List<UUID> wanted = inv.getArgument(0);
            return all.stream().filter(s -> wanted.contains(s.getId())).toList();
        });
    }

    private static HttpStatus statusOf(Throwable t) {
        return HttpStatus.valueOf(((ResponseStatusException) t).getStatusCode().value());
    }

    private LiveStream ended(RecordingStatus rec, UUID replay) {
        LiveStream s = LiveStream.builder().id(UUID.randomUUID()).channelId(channelId).ownerId(owner)
                .title("Buổi " + all.size()).source(LiveSource.BROWSER).status(LiveStatus.ENDED).chatEnabled(true)
                .createdAt(LocalDateTime.now().minusDays(1)).startedAt(LocalDateTime.now().minusDays(1))
                .endedAt(LocalDateTime.now().minusHours(23)).peakViewers(3).recordingStatus(rec).replayVideoId(replay)
                .thumbnailUrl(THUMB).build();
        all.add(s);
        return s;
    }

    private List<UUID> ids(LiveStream... streams) {
        return java.util.Arrays.stream(streams).map(LiveStream::getId).toList();
    }

    // ------------------------------------------------------------ danh sách

    @Test
    void theStudioListShowsEveryEndedStreamOfTheChannelWithItsRecordingState() {
        LiveStream withVideo = ended(RecordingStatus.DONE, UUID.randomUUID());
        LiveStream without = ended(RecordingStatus.NONE, null);
        when(repo.findByChannelIdAndStatusOrderByEndedAtDesc(channelId, LiveStatus.ENDED, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(withVideo, without), PageRequest.of(0, 20), 2));
        when(repo.replayStatuses(List.of(withVideo.getReplayVideoId())))
                .thenReturn(java.util.Collections.singletonList(new Object[] {withVideo.getReplayVideoId(), ActiveStatus.PRIVATE}));

        MyPastLivesResponse res = service.myPast(owner, PageRequest.of(0, 20));

        assertThat(res.total()).isEqualTo(2);
        assertThat(res.hasMore()).isFalse();
        assertThat(res.items()).extracting("id").containsExactly(withVideo.getId(), without.getId());
        assertThat(res.items().get(0).replayVideoId()).isEqualTo(withVideo.getReplayVideoId());
        assertThat(res.items().get(0).recordingStatus()).isEqualTo(RecordingStatus.DONE);
        assertThat(res.items().get(0).replayStatus()).isEqualTo(ActiveStatus.PRIVATE);
        // ảnh bìa dự phòng của kênh, như thẻ công khai
        assertThat(res.items()).extracting("channelBackground").containsOnly(BANNER);
        assertThat(res.items().get(1).replayVideoId()).isNull();
        assertThat(res.items().get(1).peakViewers()).isEqualTo(3);
    }

    @Test
    void aReplayVideoThatNoLongerExistsOrWasDeletedCountsAsNoReplay() {
        LiveStream gone = ended(RecordingStatus.DONE, UUID.randomUUID());
        LiveStream deleted = ended(RecordingStatus.DONE, UUID.randomUUID());
        when(repo.findByChannelIdAndStatusOrderByEndedAtDesc(channelId, LiveStatus.ENDED, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(gone, deleted), PageRequest.of(0, 20), 2));
        when(repo.replayStatuses(List.of(gone.getReplayVideoId(), deleted.getReplayVideoId())))
                .thenReturn(java.util.Collections.singletonList(new Object[] {deleted.getReplayVideoId(), ActiveStatus.DELETED}));

        MyPastLivesResponse res = service.myPast(owner, PageRequest.of(0, 20));

        assertThat(res.items()).extracting("replayVideoId").containsOnlyNulls();
        assertThat(res.items()).extracting("replayStatus").containsOnlyNulls();
    }

    @Test
    void aUserWithoutAChannelHasNoPastStreams() {
        UUID stranger = UUID.randomUUID();
        when(userRepo.findById(stranger)).thenReturn(Optional.of(AppUser.builder().id(stranger).build()));

        assertThat(service.myPast(stranger, PageRequest.of(0, 20)).items()).isEmpty();
    }

    @Test
    void thePublicTabOnlyAsksForStreamsWhoseReplayIsStillPublic() {
        LiveStream withVideo = ended(RecordingStatus.DONE, UUID.randomUUID());
        when(repo.findPublicReplays(channelId, LiveStatus.ENDED, VideoAccess.LISTED, PageRequest.of(0, 12)))
                .thenReturn(new PageImpl<>(List.of(withVideo), PageRequest.of(0, 12), 1));
        when(channelRepo.findAllById(any())).thenReturn(List.of());

        ChannelLivesResponse res = service.byChannel(channelId, PageRequest.of(0, 12));

        assertThat(res.past()).extracting("id").containsExactly(withVideo.getId());
        verify(repo, never()).findByChannelIdAndStatusOrderByEndedAtDesc(any(), any(), any());
    }

    // ------------------------------------------------------------ xoá

    @Test
    void deletingRemovesTheStreamsTheirPollsAndNotificationsButKeepsTheReplayVideoByDefault() {
        UUID replay = UUID.randomUUID();
        LiveStream withVideo = ended(RecordingStatus.DONE, replay);
        LiveStream without = ended(RecordingStatus.SKIPPED, null);

        DeleteLivesResponse res = service.deletePast(owner, ids(withVideo, without), false);

        assertThat(res).isEqualTo(new DeleteLivesResponse(2, 0));
        verify(polls).deleteOf(ids(withVideo, without));
        verify(notifications).removeLiveNotifications(ids(withVideo, without));
        verify(repo).deleteAllById(ids(withVideo, without));
        verify(rateLimiter).check(Limit.LIVE_DELETE, owner);
        verify(videos, never()).deleteVideo(any());
        // ảnh của buổi có video xem lại là ảnh của video, chỉ buổi không có video mới xoá ảnh
        verify(cloudinary).deleteImage(THUMB);
    }

    @Test
    void theThumbnailOfAStreamWhoseReplayIsKeptIsKept() {
        LiveStream withVideo = ended(RecordingStatus.DONE, UUID.randomUUID());

        service.deletePast(owner, ids(withVideo), false);

        verify(cloudinary, never()).deleteImage(any());
    }

    @Test
    void whenAskedTheReplayVideosAreDeletedToo() {
        UUID replay = UUID.randomUUID();
        LiveStream withVideo = ended(RecordingStatus.DONE, replay);
        LiveStream without = ended(RecordingStatus.NONE, null);

        DeleteLivesResponse res = service.deletePast(owner, ids(withVideo, without), true);

        assertThat(res).isEqualTo(new DeleteLivesResponse(2, 1));
        verify(videos).deleteVideo(replay);
        verify(repo).deleteAllById(ids(withVideo, without));
    }

    @Test
    void aReplayVideoThatWasAlreadyDeletedDoesNotStopTheRest() {
        UUID gone = UUID.randomUUID();
        LiveStream a = ended(RecordingStatus.DONE, gone);
        LiveStream b = ended(RecordingStatus.NONE, null);
        doThrow(new IllegalStateException("not found")).when(videos).deleteVideo(gone);

        DeleteLivesResponse res = service.deletePast(owner, ids(a, b), true);

        assertThat(res).isEqualTo(new DeleteLivesResponse(2, 0));
        verify(repo).deleteAllById(ids(a, b));
    }

    @Test
    void onlyTheOwnerCanDeleteAndNothingIsDeletedWhenOneStreamIsNotTheirs() {
        LiveStream mine = ended(RecordingStatus.NONE, null);
        LiveStream theirs = ended(RecordingStatus.NONE, null);
        theirs.setOwnerId(UUID.randomUUID());

        assertThatThrownBy(() -> service.deletePast(owner, ids(mine, theirs), true))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.FORBIDDEN));
        verify(repo, never()).deleteAllById(any());
        verify(videos, never()).deleteVideo(any());
    }

    @Test
    void aStreamThatIsNotFinishedCannotBeDeleted() {
        LiveStream live = ended(RecordingStatus.NONE, null);
        live.setStatus(LiveStatus.LIVE);

        assertThatThrownBy(() -> service.deletePast(owner, ids(live), false))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.CONFLICT));
        verify(repo, never()).deleteAllById(any());
    }

    @Test
    void aStreamStillWaitingForItsRecordingUploadCannotBeDeleted() {
        LiveStream waiting = ended(RecordingStatus.STOPPING, null);
        LiveStream fine = ended(RecordingStatus.NONE, null);

        assertThatThrownBy(() -> service.deletePast(owner, ids(fine, waiting), false))
                .satisfies(e -> {
                    assertThat(statusOf(e)).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(((ResponseStatusException) e).getReason()).contains("đang chờ bản ghi");
                });
        verify(repo, never()).deleteAllById(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void idsThatNoLongerExistAreSkipped() {
        LiveStream a = ended(RecordingStatus.NONE, null);

        DeleteLivesResponse res = service.deletePast(owner, List.of(a.getId(), UUID.randomUUID()), false);

        assertThat(res.deleted()).isEqualTo(1);
        ArgumentCaptor<List<UUID>> deleted = ArgumentCaptor.forClass(List.class);
        verify(repo).deleteAllById(deleted.capture());
        assertThat(deleted.getValue()).containsExactly(a.getId());
    }

    @Test
    void anEmptyOrTooLongSelectionIsRefused() {
        assertThatThrownBy(() -> service.deletePast(owner, List.of(), false))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.deletePast(owner, null, false))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.BAD_REQUEST));
        List<UUID> many = new ArrayList<>();
        for (int i = 0; i <= LiveStreamService.DELETE_MAX; i++) {
            many.add(UUID.randomUUID());
        }
        assertThatThrownBy(() -> service.deletePast(owner, many, false))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(repo, never()).deleteAllById(any());
    }
}
