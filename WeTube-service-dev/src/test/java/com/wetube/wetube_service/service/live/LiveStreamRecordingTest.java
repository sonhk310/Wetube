package com.wetube.wetube_service.service.live;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
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
import com.wetube.wetube_service.security.RateLimiter.Limit;
import com.wetube.wetube_service.service.CloudinaryService;
import com.wetube.wetube_service.service.CloudinaryService.UploadedVideo;
import com.wetube.wetube_service.service.live.LiveKitGateway.Participant;
import com.wetube.wetube_service.service.notification.NotificationService;
import com.wetube.wetube_service.service.video.TopicCatalog;
import com.wetube.wetube_service.service.video.VideoService;
import com.wetube.wetube_service.service.video.VideoService.LiveReplay;

/**
 * Ghi hình buổi phát (L2b): trình duyệt của chủ kênh ghi rồi tải lên Cloudinary. Hạn mức, trạng thái, tạo video xem
 * lại. Giờ được truyền vào nên test không chờ.
 */
class LiveStreamRecordingTest {

    private static final String THUMB = "https://res.cloudinary.com/x/image/upload/t.jpg";
    private static final String VIDEO_URL = "https://res.cloudinary.com/x/video/upload/q_auto,vc_h264/rec.mp4";

    private final LiveStreamRepository repo = mock(LiveStreamRepository.class);
    private final UserRepository userRepo = mock(UserRepository.class);
    private final ChannelRepository channelRepo = mock(ChannelRepository.class);
    private final LiveKitGateway gateway = mock(LiveKitGateway.class);
    private final RateLimiter rateLimiter = mock(RateLimiter.class);
    private final LiveEventPublisher publisher = mock(LiveEventPublisher.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final CloudinaryService cloudinary = mock(CloudinaryService.class);
    private final TopicCatalog topics = mock(TopicCatalog.class);
    private final VideoService videos = mock(VideoService.class);
    private final TrackingTransactionManager tm = new TrackingTransactionManager();
    private final LiveRecordingProperties props = new LiveRecordingProperties();

    private final UUID owner = UUID.randomUUID();
    private final UUID channelId = UUID.randomUUID();
    private final LocalDateTime now = LocalDateTime.of(2026, 10, 15, 12, 0, 0);
    private final List<LiveStream> all = new ArrayList<>();
    private final MockMultipartFile webm = new MockMultipartFile("file", "ghi-hinh.webm", "video/webm", new byte[] {1, 2, 3});
    private LiveStreamService service;

    @BeforeEach
    void setUp() {
        props.setChannelMinutes(15);
        props.setTotalMinutes(55);

        service = new LiveStreamService(repo, userRepo, channelRepo, gateway, rateLimiter, publisher,
                notifications, mock(LivePollService.class), cloudinary, props, topics, videos,
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
        s.setRecordingStartedAt(startedAt);
        return s;
    }

    /** Buổi đã kết thúc, ghi 10 phút theo đồng hồ, đang chờ trình duyệt tải bản ghi lên. */
    private LiveStream stoppingStream(LocalDateTime stoppedAt) {
        LiveStream s = stream(LiveStatus.ENDED, RecordingStatus.STOPPING, true);
        s.setEndedAt(stoppedAt);
        s.setRecordingStartedAt(stoppedAt.minusMinutes(10));
        s.setRecordingStoppedAt(stoppedAt);
        s.setRecordingSeconds(600);
        return s;
    }

    private void givenUploaded(Float duration) {
        when(cloudinary.uploadLiveRecording(webm)).thenReturn(new UploadedVideo(VIDEO_URL, 1280, 720, duration));
    }

    private void givenPublisherPresent() {
        when(gateway.participants(any())).thenReturn(List.of(new Participant("u:" + owner, true)));
    }

    private static CreateLiveRequest recordRequest(LiveSource source, String topic) {
        return new CreateLiveRequest("Buổi có ghi hình", "mô tả", source, null, true, true, topic);
    }

    // ------------------------------------------------------------ tạo buổi

    @Test
    void recordingIsOnByDefault() {
        assertThat(new LiveRecordingProperties().isEnabled()).isTrue();
    }

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
        LiveStream s = recordingStream(now.minusMinutes(10));
        when(repo.sumRecordingSecondsByChannel(eq(channelId), any(), any())).thenReturn(4 * 60L);
        givenPublisherPresent();

        service.refreshAll(now);

        ArgumentCaptor<LocalDateTime> from = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> to = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repo).sumRecordingSecondsByChannel(eq(channelId), from.capture(), to.capture());
        assertThat(from.getValue()).isEqualTo(LocalDateTime.of(2026, 10, 1, 0, 0));
        assertThat(to.getValue()).isEqualTo(LocalDateTime.of(2026, 11, 1, 0, 0));
        assertThat(s.getRecordingStatus()).isEqualTo(RecordingStatus.RECORDING);
    }

    // ------------------------------------------------------------ bắt đầu ghi

    @Test
    void goingLiveStartsTheRecordingWhenItWasAsked() {
        LiveStream s = stream(LiveStatus.CREATED, RecordingStatus.NONE, true);
        s.setStartedAt(null);
        givenPublisherPresent();

        service.refreshAll(now);

        assertThat(s.getStatus()).isEqualTo(LiveStatus.LIVE);
        assertThat(s.getRecordingStatus()).isEqualTo(RecordingStatus.RECORDING);
        assertThat(s.getRecordingStartedAt()).isEqualTo(now);
        verify(notifications).notifyLiveStarted(s.getId(), owner, channelId, "Buổi phát");
    }

    @Test
    void goingLiveWithoutAskingDoesNotRecord() {
        LiveStream s = stream(LiveStatus.CREATED, RecordingStatus.NONE, false);
        s.setStartedAt(null);
        givenPublisherPresent();

        service.refreshAll(now);

        assertThat(s.getStatus()).isEqualTo(LiveStatus.LIVE);
        assertThat(s.getRecordingStatus()).isEqualTo(RecordingStatus.NONE);
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
    }

    // ------------------------------------------------------------ dừng ghi

    @Test
    void recordingStopsWhenTheChannelReachesItsShareButTheStreamKeepsGoing() {
        LiveStream s = recordingStream(now.minusMinutes(15));
        givenPublisherPresent();

        service.refreshAll(now);

        assertThat(s.getRecordingStatus()).isEqualTo(RecordingStatus.STOPPING);
        assertThat(s.getRecordingMessage()).contains("Đã ghi đủ phần ghi hình của tháng");
        assertThat(s.getRecordingSeconds()).isEqualTo(15 * 60);
        assertThat(s.getStatus()).isEqualTo(LiveStatus.LIVE);
    }

    @Test
    void recordingKeepsGoingWhileThereIsShareLeft() {
        LiveStream s = recordingStream(now.minusMinutes(10));
        givenPublisherPresent();

        service.refreshAll(now);

        assertThat(s.getRecordingStatus()).isEqualTo(RecordingStatus.RECORDING);
    }

    @Test
    void recordingStopsWhenWeTubeAsAWholeReachesItsTotal() {
        LiveStream s = recordingStream(now.minusMinutes(5));
        when(repo.sumRecordingSeconds(any(), any())).thenReturn(50 * 60L);
        givenPublisherPresent();

        service.refreshAll(now);

        assertThat(s.getRecordingStatus()).isEqualTo(RecordingStatus.STOPPING);
    }

    @Test
    void endingTheStreamWaitsForTheBrowserToUploadTheRecording() {
        LiveStream s = recordingStream(LocalDateTime.now().minusMinutes(5));

        service.end(owner, false, s.getId());

        assertThat(s.getStatus()).isEqualTo(LiveStatus.ENDED);
        assertThat(s.getRecordingStatus()).isEqualTo(RecordingStatus.STOPPING);
        assertThat(s.getRecordingSeconds()).isBetween(299, 301);
    }

    // ------------------------------------------------------------ chờ bản ghi

    @Test
    void aRecordingNotYetUploadedIsWaitedFor() {
        LiveStream s = stoppingStream(now.minusHours(5).minusMinutes(59));

        service.refreshAll(now);

        assertThat(s.getRecordingStatus()).isEqualTo(RecordingStatus.STOPPING);
        assertThat(s.getRecordingSeconds()).isEqualTo(600);
    }

    @Test
    void aRecordingNeverUploadedWithinSixHoursIsGivenUpAndDoesNotCountAgainstTheShare() {
        LiveStream s = stoppingStream(now.minusHours(6).minusMinutes(1));

        service.refreshAll(now);

        assertThat(s.getRecordingStatus()).isEqualTo(RecordingStatus.FAILED);
        assertThat(s.getRecordingMessage()).contains("Không nhận được bản ghi từ trình duyệt");
        assertThat(s.getRecordingSeconds()).isZero();
    }

    @Test
    void aFailureOfOneWaitingRecordingDoesNotBlockTheOthers() {
        LiveStream broken = stoppingStream(now.minusHours(7));
        LiveStream fine = stoppingStream(now.minusHours(7));
        when(repo.findById(broken.getId())).thenThrow(new IllegalStateException("db"));

        service.refreshAll(now);

        assertThat(broken.getRecordingStatus()).isEqualTo(RecordingStatus.STOPPING);
        assertThat(fine.getRecordingStatus()).isEqualTo(RecordingStatus.FAILED);
    }

    // ------------------------------------------------------------ tải bản ghi lên

    @Test
    void anUploadedRecordingBecomesAPublicVideoOfTheChannel() {
        LiveStream s = stoppingStream(now.minusMinutes(1));
        UUID videoId = UUID.randomUUID();
        givenUploaded(590.4f);
        when(videos.createLiveReplay(any())).thenReturn(videoId);

        LiveView view = service.uploadReplay(owner, s.getId(), webm, 595.0, "USER");

        assertThat(view.recordingStatus()).isEqualTo(RecordingStatus.DONE);
        assertThat(view.replayVideoId()).isEqualTo(videoId);
        assertThat(s.getRecordingUrl()).isEqualTo(VIDEO_URL);
        assertThat(s.getRecordingSeconds()).isEqualTo(590);
        verify(rateLimiter).check(Limit.LIVE_REPLAY, owner);
        ArgumentCaptor<LiveReplay> replay = ArgumentCaptor.forClass(LiveReplay.class);
        verify(videos).createLiveReplay(replay.capture());
        LiveReplay r = replay.getValue();
        assertThat(r.ownerId()).isEqualTo(owner);
        assertThat(r.title()).isEqualTo("Buổi phát");
        assertThat(r.topicId()).isEqualTo("tech.tin-tuc");
        assertThat(r.videoUrl()).isEqualTo(VIDEO_URL);
        assertThat(r.thumbnailUrl()).isEqualTo(THUMB);
        assertThat(r.durationSeconds()).isEqualTo(590f);
        // 11:00 UTC ngày 15/10 là 18:00 ngày 15/10 giờ Việt Nam
        assertThat(r.description()).startsWith("Đã phát trực tiếp ngày 15/10/2026").endsWith("Mô tả buổi phát");
        verify(cloudinary, never()).deleteVideo(any());
    }

    @Test
    void theReplayDateIsInVietnamTimeNotUtc() {
        LiveStream s = stoppingStream(now.minusMinutes(1));
        s.setStartedAt(LocalDateTime.of(2026, 10, 15, 20, 30)); // 03:30 ngày 16/10 giờ Việt Nam
        givenUploaded(300f);
        when(videos.createLiveReplay(any())).thenReturn(UUID.randomUUID());

        service.uploadReplay(owner, s.getId(), webm, null, "USER");

        ArgumentCaptor<LiveReplay> replay = ArgumentCaptor.forClass(LiveReplay.class);
        verify(videos).createLiveReplay(replay.capture());
        assertThat(replay.getValue().description()).startsWith("Đã phát trực tiếp ngày 16/10/2026");
    }

    @Test
    void withoutADurationFromCloudinaryTheBrowserMeasurementIsUsed() {
        LiveStream s = stoppingStream(now.minusMinutes(1));
        givenUploaded(null);
        when(videos.createLiveReplay(any())).thenReturn(UUID.randomUUID());

        service.uploadReplay(owner, s.getId(), webm, 412.6, "USER");

        assertThat(s.getRecordingSeconds()).isEqualTo(413);
    }

    @Test
    void theCountedDurationNeverExceedsWhatTheServerClockSaw() {
        LiveStream s = stoppingStream(now.minusMinutes(1)); // 600 giây theo đồng hồ
        givenUploaded(null);
        when(videos.createLiveReplay(any())).thenReturn(UUID.randomUUID());

        service.uploadReplay(owner, s.getId(), webm, 5000.0, "USER");

        assertThat(s.getRecordingSeconds()).isEqualTo(600);
    }

    @Test
    void aRecordingThatWasCutShortByTheShareKeepsItsExplanationOnceItIsPublished() {
        LiveStream s = stoppingStream(now.minusMinutes(1));
        s.setRecordingMessage("Đã ghi đủ phần ghi hình của tháng. Buổi vẫn đang phát nhưng không ghi tiếp.");
        givenUploaded(590f);
        when(videos.createLiveReplay(any())).thenReturn(UUID.randomUUID());

        service.uploadReplay(owner, s.getId(), webm, null, "USER");

        assertThat(s.getRecordingStatus()).isEqualTo(RecordingStatus.DONE);
        assertThat(s.getRecordingMessage()).contains("Đã ghi đủ phần ghi hình của tháng");
    }

    @Test
    void aRecordingUnderThirtySecondsIsNotPublishedAndItsFileIsRemoved() {
        LiveStream s = stoppingStream(now.minusMinutes(1));
        givenUploaded(20f);

        service.uploadReplay(owner, s.getId(), webm, null, "USER");

        assertThat(s.getRecordingStatus()).isEqualTo(RecordingStatus.SKIPPED);
        assertThat(s.getRecordingMessage()).contains("dưới 30 giây");
        verify(videos, never()).createLiveReplay(any());
        verify(cloudinary).deleteVideo(VIDEO_URL);
    }

    @Test
    void ifThePublishingOfTheVideoFailsTheReasonIsKeptAndTheFileIsRemoved() {
        LiveStream s = stoppingStream(now.minusMinutes(1));
        givenUploaded(600f);
        when(videos.createLiveReplay(any())).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Kênh của bạn đang bị quản trị viên ẩn"));

        service.uploadReplay(owner, s.getId(), webm, null, "USER");

        assertThat(s.getRecordingStatus()).isEqualTo(RecordingStatus.FAILED);
        assertThat(s.getRecordingMessage()).contains("Không đăng được bản ghi thành video")
                .contains("Kênh của bạn đang bị quản trị viên ẩn");
        assertThat(s.getReplayVideoId()).isNull();
        verify(cloudinary).deleteVideo(VIDEO_URL);
    }

    @Test
    void ifCloudinaryFailsTheStreamKeepsWaitingSoTheOwnerCanRetry() {
        LiveStream s = stoppingStream(now.minusMinutes(1));
        when(cloudinary.uploadLiveRecording(webm)).thenThrow(new RuntimeException("Upload to Cloudinary failed"))
                .thenReturn(new UploadedVideo(VIDEO_URL, 1280, 720, 600f));
        when(videos.createLiveReplay(any())).thenReturn(UUID.randomUUID());

        assertThatThrownBy(() -> service.uploadReplay(owner, s.getId(), webm, null, "USER"))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.BAD_GATEWAY));
        assertThat(s.getRecordingStatus()).isEqualTo(RecordingStatus.STOPPING);

        service.uploadReplay(owner, s.getId(), webm, null, "USER");
        assertThat(s.getRecordingStatus()).isEqualTo(RecordingStatus.DONE);
    }

    @Test
    void onlyTheOwnerCanUploadTheRecording() {
        LiveStream s = stoppingStream(now.minusMinutes(1));

        assertThatThrownBy(() -> service.uploadReplay(UUID.randomUUID(), s.getId(), webm, null, "USER"))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.FORBIDDEN));
        verify(cloudinary, never()).uploadLiveRecording(any());
    }

    @Test
    void anUploadIsRefusedWhenTheStreamIsNotWaitingForOne() {
        LiveStream live = recordingStream(now.minusMinutes(5));
        LiveStream done = stoppingStream(now.minusMinutes(1));
        done.setRecordingStatus(RecordingStatus.DONE);
        LiveStream notRecorded = stream(LiveStatus.ENDED, RecordingStatus.NONE, false);
        LiveStream givenUp = stoppingStream(now.minusMinutes(1));
        givenUp.setRecordingStatus(RecordingStatus.FAILED);

        for (LiveStream s : List.of(live, done, notRecorded, givenUp)) {
            assertThatThrownBy(() -> service.uploadReplay(owner, s.getId(), webm, null, "USER"))
                    .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.CONFLICT));
        }
        assertThatThrownBy(() -> service.uploadReplay(owner, done.getId(), webm, null, "USER"))
                .hasMessageContaining("đã có video xem lại");
        verify(cloudinary, never()).uploadLiveRecording(any());
    }

    @Test
    void theUploadMustBeANonEmptyVideo() {
        LiveStream s = stoppingStream(now.minusMinutes(1));

        assertThatThrownBy(() -> service.uploadReplay(owner, s.getId(),
                new MockMultipartFile("file", "a.txt", "text/plain", new byte[] {1}), null, "USER"))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.uploadReplay(owner, s.getId(),
                new MockMultipartFile("file", "a.webm", "video/webm", new byte[0]), null, "USER"))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(cloudinary, never()).uploadLiveRecording(any());
    }

    @Test
    void aSecondUploadWhileTheFirstIsStillRunningIsRefused() {
        LiveStream s = stoppingStream(now.minusMinutes(1));
        List<Throwable> second = new ArrayList<>();
        when(cloudinary.uploadLiveRecording(webm)).thenAnswer(inv -> {
            try {
                service.uploadReplay(owner, s.getId(), webm, null, "USER");
            } catch (ResponseStatusException e) {
                second.add(e);
            }
            return new UploadedVideo(VIDEO_URL, 1280, 720, 600f);
        });
        when(videos.createLiveReplay(any())).thenReturn(UUID.randomUUID());

        service.uploadReplay(owner, s.getId(), webm, null, "USER");

        assertThat(second).singleElement().satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.CONFLICT));
        verify(videos).createLiveReplay(any());
        assertThat(s.getRecordingStatus()).isEqualTo(RecordingStatus.DONE);
    }

    @Test
    void ifTheWaitWasGivenUpDuringTheUploadTheFileIsRemoved() {
        LiveStream s = stoppingStream(now.minusMinutes(1));
        when(cloudinary.uploadLiveRecording(webm)).thenAnswer(inv -> {
            s.setRecordingStatus(RecordingStatus.FAILED); // job 6 giờ chạy giữa chừng
            return new UploadedVideo(VIDEO_URL, 1280, 720, 600f);
        });

        assertThatThrownBy(() -> service.uploadReplay(owner, s.getId(), webm, null, "USER"))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.CONFLICT));
        verify(cloudinary).deleteVideo(VIDEO_URL);
        verify(videos, never()).createLiveReplay(any());
    }

    // ------------------------------------------------------------ ai thấy gì

    @Test
    void onlyTheOwnerSeesWhyThereIsNoReplay() {
        LiveStream s = stoppingStream(now.minusMinutes(5));
        s.setRecordingStatus(RecordingStatus.FAILED);
        s.setRecordingMessage("Không nhận được bản ghi từ trình duyệt của chủ kênh.");

        assertThat(service.get(s.getId(), owner).recordingMessage()).contains("Không nhận được bản ghi");
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
