package com.wetube.wetube_service.service.live;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.wetube.wetube_service.dto.LiveRecordingProperties;
import com.wetube.wetube_service.dto.live.CreateLiveRequest;
import com.wetube.wetube_service.dto.live.LiveRecordingInfo;
import com.wetube.wetube_service.dto.live.LiveView;
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
import com.wetube.wetube_service.service.CloudinaryService;
import com.wetube.wetube_service.service.live.LiveKitGateway.Participant;
import com.wetube.wetube_service.service.live.LiveKitGateway.RecordingInfo;
import com.wetube.wetube_service.service.notification.NotificationService;
import com.wetube.wetube_service.service.video.TopicCatalog;
import com.wetube.wetube_service.service.video.VideoService;
import com.wetube.wetube_service.service.video.VideoService.LiveReplay;

/** Ghi hình buổi phát (L2): hạn mức, bắt đầu / dừng, tạo video xem lại. Giờ được truyền vào nên test không chờ. */
class LiveStreamRecordingTest {

    private static final String PUBLIC_URL = "https://pub.example";
    private static final String THUMB = "https://res.cloudinary.com/x/image/upload/t.jpg";

    private final LiveStreamRepository repo = mock(LiveStreamRepository.class);
    private final UserRepository userRepo = mock(UserRepository.class);
    private final ChannelRepository channelRepo = mock(ChannelRepository.class);
    private final LiveKitGateway gateway = mock(LiveKitGateway.class);
    private final LiveEventPublisher publisher = mock(LiveEventPublisher.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final TopicCatalog topics = mock(TopicCatalog.class);
    private final VideoService videos = mock(VideoService.class);
    private final TrackingTransactionManager tm = new TrackingTransactionManager();
    private final LiveRecordingProperties props = new LiveRecordingProperties();

    private final UUID owner = UUID.randomUUID();
    private final UUID channelId = UUID.randomUUID();
    private final LocalDateTime now = LocalDateTime.of(2026, 10, 15, 12, 0, 0);
    private final List<LiveStream> all = new ArrayList<>();
    private LiveStreamService service;

    @BeforeEach
    void setUp() {
        props.setEnabled(true);
        props.setS3Endpoint("https://acc.r2.cloudflarestorage.com");
        props.setS3Bucket("wetube-live");
        props.setS3AccessKey("key");
        props.setS3Secret("secret");
        props.setPublicUrl(PUBLIC_URL + "/");
        props.setChannelMinutes(15);
        props.setTotalMinutes(55);

        service = new LiveStreamService(repo, userRepo, channelRepo, gateway, mock(RateLimiter.class), publisher,
                notifications, mock(LivePollService.class), mock(CloudinaryService.class), props, topics, videos,
                List.of(mock(LiveCleanup.class)), tm);

        Channel channel = Channel.builder().id(channelId).name("Kênh Sơn").status(ActiveStatus.ACTIVE).build();
        when(userRepo.findById(owner)).thenReturn(Optional.of(
                AppUser.builder().id(owner).email("son@example.com").name("Sơn").channel(channel).build()));
        when(channelRepo.findById(channelId)).thenReturn(Optional.of(channel));
        when(repo.save(any(LiveStream.class))).thenAnswer(inv -> {
            LiveStream s = inv.getArgument(0);
            if (s.getId() == null) {
                s.setId(UUID.randomUUID());
            }
            return s;
        });
        when(repo.findFirstByChannelIdAndStatusIn(any(), any())).thenReturn(Optional.empty());
        when(repo.findByStatusIn(any())).thenAnswer(inv -> {
            Collection<LiveStatus> wanted = inv.getArgument(0);
            return all.stream().filter(s -> wanted.contains(s.getStatus())).toList();
        });
        when(repo.findByRecordingStatusIn(any())).thenAnswer(inv -> {
            Collection<RecordingStatus> wanted = inv.getArgument(0);
            return all.stream().filter(s -> wanted.contains(recording(s))).toList();
        });
        when(topics.exists("tech.tin-tuc")).thenReturn(true);
        when(gateway.wsUrl()).thenReturn("wss://livekit.test");
    }

    private static RecordingStatus recording(LiveStream s) {
        return s.getRecordingStatus() == null ? RecordingStatus.NONE : s.getRecordingStatus();
    }

    private static HttpStatus statusOf(Throwable t) {
        return HttpStatus.valueOf(((ResponseStatusException) t).getStatusCode().value());
    }

    private LiveStream stream(LiveStatus status, RecordingStatus rec, boolean recordingEnabled) {
        LiveStream s = LiveStream.builder().id(UUID.randomUUID()).channelId(channelId).ownerId(owner)
                .title("Buổi phát").description("Mô tả buổi phát").source(LiveSource.BROWSER).status(status)
                .chatEnabled(true).livekitRoom("live-" + UUID.randomUUID())
                .createdAt(status == LiveStatus.CREATED ? now.minusMinutes(5) : now.minusHours(2))
                .startedAt(status == LiveStatus.CREATED ? null : now.minusHours(1))
                .lastPublisherAt(now.minusSeconds(5)).recordingEnabled(recordingEnabled).topicId("tech.tin-tuc")
                .recordingStatus(rec).thumbnailUrl(THUMB).build();
        when(repo.findById(s.getId())).thenReturn(Optional.of(s));
        all.add(s);
        return s;
    }

    private LiveStream recordingStream(LocalDateTime startedAt) {
        LiveStream s = stream(LiveStatus.LIVE, RecordingStatus.RECORDING, true);
        s.setEgressId("EG_1");
        s.setRecordingStartedAt(startedAt);
        return s;
    }

    private LiveStream stoppingStream(LocalDateTime stoppedAt) {
        LiveStream s = stream(LiveStatus.ENDED, RecordingStatus.STOPPING, true);
        s.setEndedAt(stoppedAt);
        s.setEgressId("EG_1");
        s.setRecordingStartedAt(stoppedAt.minusMinutes(10));
        s.setRecordingStoppedAt(stoppedAt);
        s.setRecordingSeconds(600);
        return s;
    }

    private void givenPublisherPresent() {
        when(gateway.participants(any())).thenReturn(List.of(new Participant("u:" + owner, true)));
    }

    private static CreateLiveRequest recordRequest(LiveSource source, String topic) {
        return new CreateLiveRequest("Buổi có ghi hình", "mô tả", source, null, true, true, topic);
    }

    // ------------------------------------------------------------ tạo buổi

    @Test
    void recordingCanBeAskedOnlyForBrowserStreams() {
        assertThatThrownBy(() -> service.create(owner, recordRequest(LiveSource.ENCODER, "tech.tin-tuc")))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(gateway, never()).createRoom(anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void recordingNeedsAValidTopic() {
        for (String bad : new String[] {null, "", "  ", "khong.co"}) {
            assertThatThrownBy(() -> service.create(owner, recordRequest(LiveSource.BROWSER, bad)))
                    .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.BAD_REQUEST));
        }
    }

    @Test
    void recordingIsRefusedWhenTheServerHasItSwitchedOff() {
        props.setEnabled(false);

        assertThatThrownBy(() -> service.create(owner, recordRequest(LiveSource.BROWSER, "tech.tin-tuc")))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void recordingIsRefusedWhenTheChannelHasUsedItsMonthlyShare() {
        when(repo.sumRecordingSecondsByChannel(eq(channelId), any(), any())).thenReturn(15 * 60L);

        assertThatThrownBy(() -> service.create(owner, recordRequest(LiveSource.BROWSER, "tech.tin-tuc")))
                .satisfies(e -> {
                    assertThat(statusOf(e)).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(((ResponseStatusException) e).getReason()).contains("Kênh đã dùng hết 15 phút");
                });
    }

    @Test
    void recordingIsRefusedWhenWeTubeHasUsedItsMonthlyTotal() {
        when(repo.sumRecordingSeconds(any(), any())).thenReturn(55 * 60L);

        assertThatThrownBy(() -> service.create(owner, recordRequest(LiveSource.BROWSER, "tech.tin-tuc")))
                .satisfies(e -> {
                    assertThat(statusOf(e)).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(((ResponseStatusException) e).getReason()).contains("WeTube đã dùng hết");
                });
    }

    @Test
    void streamWithRecordingKeepsTheFlagAndTheTopic() {
        LiveView view = service.create(owner, recordRequest(LiveSource.BROWSER, " tech.tin-tuc "));

        assertThat(view.recordingEnabled()).isTrue();
        assertThat(view.recordingStatus()).isEqualTo(RecordingStatus.NONE);
        ArgumentCaptor<LiveStream> saved = ArgumentCaptor.forClass(LiveStream.class);
        verify(repo).save(saved.capture());
        assertThat(saved.getValue().getTopicId()).isEqualTo("tech.tin-tuc");
    }

    @Test
    void streamWithoutRecordingNeedsNoTopicAndHasNoRecording() {
        LiveView view = service.create(owner,
                new CreateLiveRequest("Buổi thường", null, LiveSource.BROWSER, null, true, null, null));

        assertThat(view.recordingEnabled()).isFalse();
        assertThat(view.recordingStatus()).isEqualTo(RecordingStatus.NONE);
    }

    // ------------------------------------------------------------ hạn mức hiển thị ở form

    @Test
    void recordingInfoIsOffWhenTheServerDoesNotHaveIt() {
        props.setEnabled(false);

        assertThat(service.recordingInfo(owner)).isEqualTo(new LiveRecordingInfo(false, 0, null));
    }

    @Test
    void recordingInfoShowsWhatIsLeftAndWhoLimitsIt() {
        when(repo.sumRecordingSecondsByChannel(eq(channelId), any(), any())).thenReturn(5 * 60L);
        assertThat(service.recordingInfo(owner)).isEqualTo(new LiveRecordingInfo(true, 10 * 60L, null));

        when(repo.sumRecordingSecondsByChannel(eq(channelId), any(), any())).thenReturn(15 * 60L);
        assertThat(service.recordingInfo(owner)).isEqualTo(new LiveRecordingInfo(true, 0, "CHANNEL"));

        when(repo.sumRecordingSecondsByChannel(eq(channelId), any(), any())).thenReturn(0L);
        when(repo.sumRecordingSeconds(any(), any())).thenReturn(50 * 60L);
        // phần chung còn 5 phút, thấp hơn phần của kênh (15 phút)
        assertThat(service.recordingInfo(owner)).isEqualTo(new LiveRecordingInfo(true, 5 * 60L, null));

        when(repo.sumRecordingSeconds(any(), any())).thenReturn(55 * 60L);
        assertThat(service.recordingInfo(owner)).isEqualTo(new LiveRecordingInfo(true, 0, "TOTAL"));
    }

    @Test
    void theMonthlyShareIsCountedOverTheCalendarMonthAndIncludesTheRecordingInProgress() {
        // đang ghi 10 phút, kênh đã có 4 phút đã chốt trong tháng: còn 15 - 4 - 10 = 1 phút
        recordingStream(now.minusMinutes(10));
        when(repo.sumRecordingSecondsByChannel(eq(channelId), any(), any())).thenReturn(4 * 60L);
        givenPublisherPresent();
        when(gateway.recording("EG_1")).thenReturn(new RecordingInfo(false, false, 0, ""));

        service.refreshAll(now);

        ArgumentCaptor<LocalDateTime> from = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> to = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repo).sumRecordingSecondsByChannel(eq(channelId), from.capture(), to.capture());
        assertThat(from.getValue()).isEqualTo(LocalDateTime.of(2026, 10, 1, 0, 0));
        assertThat(to.getValue()).isEqualTo(LocalDateTime.of(2026, 11, 1, 0, 0));
        verify(gateway, never()).stopRecording(any());
    }

    // ------------------------------------------------------------ bắt đầu ghi

    @Test
    void goingLiveStartsTheRecordingWhenItWasAsked() {
        LiveStream s = stream(LiveStatus.CREATED, RecordingStatus.NONE, true);
        s.setStartedAt(null);
        givenPublisherPresent();
        when(gateway.startRecording(s.getLivekitRoom(), "live/" + s.getId() + ".mp4")).thenReturn("EG_9");

        service.refreshAll(now);

        assertThat(s.getStatus()).isEqualTo(LiveStatus.LIVE);
        assertThat(s.getRecordingStatus()).isEqualTo(RecordingStatus.RECORDING);
        assertThat(s.getEgressId()).isEqualTo("EG_9");
        assertThat(s.getRecordingStartedAt()).isEqualTo(now);
    }

    @Test
    void goingLiveWithoutAskingDoesNotRecord() {
        LiveStream s = stream(LiveStatus.CREATED, RecordingStatus.NONE, false);
        s.setStartedAt(null);
        givenPublisherPresent();

        service.refreshAll(now);

        assertThat(s.getStatus()).isEqualTo(LiveStatus.LIVE);
        verify(gateway, never()).startRecording(any(), any());
        assertThat(s.getRecordingStatus()).isEqualTo(RecordingStatus.NONE);
    }

    @Test
    void ifLivekitRefusesToRecordTheStreamStillGoesLiveWithAReason() {
        LiveStream s = stream(LiveStatus.CREATED, RecordingStatus.NONE, true);
        s.setStartedAt(null);
        givenPublisherPresent();
        when(gateway.startRecording(any(), any())).thenThrow(new LiveKitUnavailableException("quota", true));

        service.refreshAll(now);

        assertThat(s.getStatus()).isEqualTo(LiveStatus.LIVE);
        assertThat(s.getRecordingStatus()).isEqualTo(RecordingStatus.FAILED);
        assertThat(s.getRecordingMessage()).contains("Hết hạn mức ghi hình của LiveKit").contains("Buổi vẫn phát");
        verify(notifications).notifyLiveStarted(s.getId(), owner, channelId, "Buổi phát");
    }

    @Test
    void ifTheMonthlyShareRanOutBeforeGoingLiveTheStreamIsNotRecorded() {
        LiveStream s = stream(LiveStatus.CREATED, RecordingStatus.NONE, true);
        s.setStartedAt(null);
        givenPublisherPresent();
        when(repo.sumRecordingSecondsByChannel(eq(channelId), any(), any())).thenReturn(15 * 60L);

        service.refreshAll(now);

        assertThat(s.getStatus()).isEqualTo(LiveStatus.LIVE);
        assertThat(s.getRecordingStatus()).isEqualTo(RecordingStatus.SKIPPED);
        assertThat(s.getRecordingMessage()).contains("Kênh đã dùng hết").contains("Buổi vẫn phát");
        verify(gateway, never()).startRecording(any(), any());
    }

    // ------------------------------------------------------------ dừng ghi

    @Test
    void recordingStopsWhenTheChannelReachesItsShareButTheStreamKeepsGoing() {
        LiveStream s = recordingStream(now.minusMinutes(15));
        givenPublisherPresent();
        when(gateway.recording("EG_1")).thenReturn(new RecordingInfo(false, false, 0, ""));

        service.refreshAll(now);

        verify(gateway).stopRecording("EG_1");
        assertThat(s.getRecordingStatus()).isEqualTo(RecordingStatus.STOPPING);
        assertThat(s.getRecordingMessage()).contains("Đã ghi đủ phần ghi hình của tháng");
        assertThat(s.getRecordingSeconds()).isEqualTo(15 * 60);
        assertThat(s.getStatus()).isEqualTo(LiveStatus.LIVE);
    }

    @Test
    void recordingKeepsGoingWhileThereIsShareLeft() {
        LiveStream s = recordingStream(now.minusMinutes(10));
        givenPublisherPresent();
        when(gateway.recording("EG_1")).thenReturn(new RecordingInfo(false, false, 0, ""));

        service.refreshAll(now);

        verify(gateway, never()).stopRecording(any());
        assertThat(s.getRecordingStatus()).isEqualTo(RecordingStatus.RECORDING);
    }

    @Test
    void recordingStopsWhenWeTubeAsAWholeReachesItsTotal() {
        recordingStream(now.minusMinutes(5));
        when(repo.sumRecordingSeconds(any(), any())).thenReturn(50 * 60L);
        givenPublisherPresent();
        when(gateway.recording("EG_1")).thenReturn(new RecordingInfo(false, false, 0, ""));

        service.refreshAll(now);

        verify(gateway).stopRecording("EG_1");
    }

    @Test
    void endingTheStreamStopsTheRecordingBeforeTheRoomIsDeleted() {
        LiveStream s = recordingStream(now.minusMinutes(5));

        service.end(owner, false, s.getId());

        InOrder order = inOrder(gateway);
        order.verify(gateway).stopRecording("EG_1");
        order.verify(gateway).deleteRoom(s.getLivekitRoom());
        assertThat(s.getStatus()).isEqualTo(LiveStatus.ENDED);
        assertThat(s.getRecordingStatus()).isEqualTo(RecordingStatus.STOPPING);
    }

    @Test
    void endingStillWorksWhenLivekitCannotBeToldToStopRecording() {
        LiveStream s = recordingStream(now.minusMinutes(5));
        doThrow(new LiveKitUnavailableException("down", false)).when(gateway).stopRecording("EG_1");

        service.end(owner, false, s.getId());

        assertThat(s.getStatus()).isEqualTo(LiveStatus.ENDED);
        assertThat(s.getRecordingStatus()).isEqualTo(RecordingStatus.STOPPING);
        verify(gateway).deleteRoom(s.getLivekitRoom());
    }

    // ------------------------------------------------------------ lấy file và tạo video

    @Test
    void aFinishedRecordingBecomesAPublicVideoOfTheChannel() {
        LiveStream s = stoppingStream(now.minusMinutes(1));
        UUID videoId = UUID.randomUUID();
        when(gateway.recording("EG_1")).thenReturn(new RecordingInfo(true, true, 612, ""));
        when(videos.createLiveReplay(any())).thenReturn(videoId);

        service.refreshAll(now);

        assertThat(s.getRecordingStatus()).isEqualTo(RecordingStatus.DONE);
        assertThat(s.getReplayVideoId()).isEqualTo(videoId);
        assertThat(s.getRecordingUrl()).isEqualTo(PUBLIC_URL + "/live/" + s.getId() + ".mp4");
        assertThat(s.getRecordingSeconds()).isEqualTo(612);
        ArgumentCaptor<LiveReplay> replay = ArgumentCaptor.forClass(LiveReplay.class);
        verify(videos).createLiveReplay(replay.capture());
        LiveReplay r = replay.getValue();
        assertThat(r.ownerId()).isEqualTo(owner);
        assertThat(r.title()).isEqualTo("Buổi phát");
        assertThat(r.topicId()).isEqualTo("tech.tin-tuc");
        assertThat(r.videoUrl()).isEqualTo(PUBLIC_URL + "/live/" + s.getId() + ".mp4");
        assertThat(r.thumbnailUrl()).isEqualTo(THUMB);
        assertThat(r.durationSeconds()).isEqualTo(612f);
        // 11:00 UTC ngày 15/10 là 18:00 ngày 15/10 giờ Việt Nam
        assertThat(r.description()).startsWith("Đã phát trực tiếp ngày 15/10/2026").endsWith("Mô tả buổi phát");
    }

    @Test
    void theReplayDateIsInVietnamTimeNotUtc() {
        LiveStream s = stoppingStream(now.minusMinutes(1));
        s.setStartedAt(LocalDateTime.of(2026, 10, 15, 20, 30)); // 03:30 ngày 16/10 giờ Việt Nam
        when(gateway.recording("EG_1")).thenReturn(new RecordingInfo(true, true, 300, ""));
        when(videos.createLiveReplay(any())).thenReturn(UUID.randomUUID());

        service.refreshAll(now);

        ArgumentCaptor<LiveReplay> replay = ArgumentCaptor.forClass(LiveReplay.class);
        verify(videos).createLiveReplay(replay.capture());
        assertThat(replay.getValue().description()).startsWith("Đã phát trực tiếp ngày 16/10/2026");
    }

    @Test
    void aRecordingThatWasCutShortByTheShareKeepsItsExplanationOnceItIsPublished() {
        LiveStream s = stoppingStream(now.minusMinutes(1));
        s.setRecordingMessage("Đã ghi đủ phần ghi hình của tháng. Buổi vẫn đang phát nhưng không ghi tiếp.");
        when(gateway.recording("EG_1")).thenReturn(new RecordingInfo(true, true, 900, ""));
        when(videos.createLiveReplay(any())).thenReturn(UUID.randomUUID());

        service.refreshAll(now);

        assertThat(s.getRecordingStatus()).isEqualTo(RecordingStatus.DONE);
        assertThat(s.getRecordingMessage()).contains("Đã ghi đủ phần ghi hình của tháng");
    }

    @Test
    void aRecordingUnderThirtySecondsIsNotPublished() {
        LiveStream s = stoppingStream(now.minusMinutes(1));
        when(gateway.recording("EG_1")).thenReturn(new RecordingInfo(true, true, 20, ""));

        service.refreshAll(now);

        assertThat(s.getRecordingStatus()).isEqualTo(RecordingStatus.SKIPPED);
        assertThat(s.getRecordingMessage()).contains("dưới 30 giây");
        verify(videos, never()).createLiveReplay(any());
    }

    @Test
    void aRecordingWithoutAFileExplainsWhy() {
        LiveStream s = stoppingStream(now.minusMinutes(1));
        when(gateway.recording("EG_1")).thenReturn(new RecordingInfo(true, false, 0, "egress quota exceeded"));

        service.refreshAll(now);

        assertThat(s.getRecordingStatus()).isEqualTo(RecordingStatus.FAILED);
        assertThat(s.getRecordingMessage()).contains("Hết hạn mức ghi hình của LiveKit");
        verify(videos, never()).createLiveReplay(any());
    }

    @Test
    void ifThePublishingOfTheVideoFailsTheReasonIsKept() {
        LiveStream s = stoppingStream(now.minusMinutes(1));
        when(gateway.recording("EG_1")).thenReturn(new RecordingInfo(true, true, 600, ""));
        when(videos.createLiveReplay(any())).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Kênh của bạn đang bị quản trị viên ẩn"));

        service.refreshAll(now);

        assertThat(s.getRecordingStatus()).isEqualTo(RecordingStatus.FAILED);
        assertThat(s.getRecordingMessage()).contains("Không đăng được bản ghi thành video")
                .contains("Kênh của bạn đang bị quản trị viên ẩn");
        assertThat(s.getReplayVideoId()).isNull();
    }

    @Test
    void aRecordingStillBeingFinishedIsWaitedFor() {
        LiveStream s = stoppingStream(now.minusMinutes(5));
        when(gateway.recording("EG_1")).thenReturn(new RecordingInfo(false, false, 0, ""));

        service.refreshAll(now);

        assertThat(s.getRecordingStatus()).isEqualTo(RecordingStatus.STOPPING);
        verify(videos, never()).createLiveReplay(any());
    }

    @Test
    void aRecordingNeverDeliveredWithinHalfAnHourIsGivenUp() {
        LiveStream s = stoppingStream(now.minusMinutes(31));
        when(gateway.recording("EG_1")).thenReturn(new RecordingInfo(false, false, 0, ""));

        service.refreshAll(now);

        assertThat(s.getRecordingStatus()).isEqualTo(RecordingStatus.FAILED);
        assertThat(s.getRecordingMessage()).contains("Không nhận được bản ghi");
    }

    @Test
    void ifLivekitCannotBeAskedTheRecordingStaysPendingForTheNextRound() {
        LiveStream s = stoppingStream(now.minusMinutes(5));
        when(gateway.recording("EG_1")).thenThrow(new LiveKitUnavailableException("down", false));

        service.refreshAll(now);

        assertThat(s.getRecordingStatus()).isEqualTo(RecordingStatus.STOPPING);
    }

    @Test
    void aRecordingThatLivekitEndsByItselfWhileTheStreamIsStillLiveIsPublished() {
        // file MP4 tối đa 3 giờ: LiveKit dừng, buổi vẫn phát
        LiveStream s = recordingStream(now.minusHours(3));
        givenPublisherPresent();
        when(gateway.recording("EG_1")).thenReturn(new RecordingInfo(true, true, 3 * 3600, "limit reached"));
        when(videos.createLiveReplay(any())).thenReturn(UUID.randomUUID());

        service.refreshAll(now);

        assertThat(s.getRecordingStatus()).isEqualTo(RecordingStatus.DONE);
        assertThat(s.getStatus()).isEqualTo(LiveStatus.LIVE);
        verify(gateway, never()).stopRecording(any());
    }

    @Test
    void aFailureOfOneStoppingRecordingDoesNotBlockTheOthers() {
        LiveStream broken = stoppingStream(now.minusMinutes(5));
        broken.setEgressId("EG_BAD");
        LiveStream fine = stoppingStream(now.minusMinutes(5));
        fine.setEgressId("EG_OK");
        when(gateway.recording("EG_BAD")).thenThrow(new LiveKitUnavailableException("down", false));
        when(gateway.recording("EG_OK")).thenReturn(new RecordingInfo(true, true, 600, ""));
        when(videos.createLiveReplay(any())).thenReturn(UUID.randomUUID());

        service.refreshAll(now);

        assertThat(broken.getRecordingStatus()).isEqualTo(RecordingStatus.STOPPING);
        assertThat(fine.getRecordingStatus()).isEqualTo(RecordingStatus.DONE);
    }

    // ------------------------------------------------------------ ai thấy gì

    @Test
    void onlyTheOwnerSeesWhyThereIsNoReplay() {
        LiveStream s = stoppingStream(now.minusMinutes(5));
        s.setRecordingStatus(RecordingStatus.FAILED);
        s.setRecordingMessage("Ghi hình bị lỗi ở máy chủ phát trực tiếp.");

        assertThat(service.get(s.getId(), owner).recordingMessage()).contains("Ghi hình bị lỗi");
        assertThat(service.get(s.getId(), UUID.randomUUID()).recordingMessage()).isNull();
        assertThat(service.get(s.getId(), null).recordingMessage()).isNull();
        assertThat(service.get(s.getId(), null).recordingStatus()).isEqualTo(RecordingStatus.FAILED);
    }

    @Test
    void theReplayVideoIsPublicOnTheEndedStream() {
        LiveStream s = stoppingStream(now.minusMinutes(5));
        UUID videoId = UUID.randomUUID();
        s.setRecordingStatus(RecordingStatus.DONE);
        s.setReplayVideoId(videoId);

        assertThat(service.get(s.getId(), null).replayVideoId()).isEqualTo(videoId);
    }

    @Test
    void oldRowsWithoutARecordingStatusReadAsNone() {
        LiveStream s = stream(LiveStatus.ENDED, null, false);

        assertThat(service.get(s.getId(), null).recordingStatus()).isEqualTo(RecordingStatus.NONE);
    }
}
