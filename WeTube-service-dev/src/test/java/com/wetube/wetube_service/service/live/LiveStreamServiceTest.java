package com.wetube.wetube_service.service.live;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.wetube.wetube_service.dto.live.CreateLiveRequest;
import com.wetube.wetube_service.dto.live.LiveCard;
import com.wetube.wetube_service.dto.live.LiveEvent;
import com.wetube.wetube_service.dto.live.LiveView;
import com.wetube.wetube_service.dto.live.PollView;
import com.wetube.wetube_service.dto.live.UpdateLiveRequest;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.channel.Channel;
import com.wetube.wetube_service.entity.live.LiveStream;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.enumeration.LiveProtocol;
import com.wetube.wetube_service.enumeration.LiveSource;
import com.wetube.wetube_service.enumeration.LiveStatus;
import com.wetube.wetube_service.repository.UserRepository;
import com.wetube.wetube_service.repository.channel.ChannelRepository;
import com.wetube.wetube_service.repository.live.LiveStreamRepository;
import com.wetube.wetube_service.security.RateLimiter;
import com.wetube.wetube_service.security.RateLimiter.Limit;
import com.wetube.wetube_service.dto.LiveRecordingProperties;
import com.wetube.wetube_service.service.CloudinaryService;
import com.wetube.wetube_service.service.video.TopicCatalog;
import com.wetube.wetube_service.service.video.VideoService;
import com.wetube.wetube_service.service.live.LiveKitGateway.IngressDetails;
import com.wetube.wetube_service.service.notification.NotificationService;

class LiveStreamServiceTest {

    private final LiveStreamRepository repo = mock(LiveStreamRepository.class);
    private final UserRepository userRepo = mock(UserRepository.class);
    private final ChannelRepository channelRepo = mock(ChannelRepository.class);
    private final LiveKitGateway gateway = mock(LiveKitGateway.class);
    private final RateLimiter rateLimiter = mock(RateLimiter.class);
    private final LiveEventPublisher publisher = mock(LiveEventPublisher.class);
    private final LiveCleanup cleanup = mock(LiveCleanup.class);
    private final LivePollService polls = mock(LivePollService.class);
    private final CloudinaryService cloudinary = mock(CloudinaryService.class);
    private final TrackingTransactionManager tm = new TrackingTransactionManager();

    private final UUID owner = UUID.randomUUID();
    private final UUID channelId = UUID.randomUUID();
    private LiveStreamService service;

    private Channel channel(ActiveStatus status) {
        return Channel.builder().id(channelId).name("Kênh Sơn").picture("avatar.png").handle("son")
                .backgroundImgUrl("banner.png").status(status).build();
    }

    private void givenOwnerWithChannel(ActiveStatus status) {
        Channel channel = channel(status);
        AppUser user = AppUser.builder().id(owner).email("son@example.com").name("Sơn").channel(channel).build();
        when(userRepo.findById(owner)).thenReturn(Optional.of(user));
        when(channelRepo.findById(channelId)).thenReturn(Optional.of(channel));
        when(channelRepo.findAllById(any())).thenReturn(List.of(channel));
    }

    private LiveStream stored(LiveStatus status) {
        LiveStream s = LiveStream.builder().id(UUID.randomUUID()).channelId(channelId).ownerId(owner).title("Buổi phát")
                .source(LiveSource.ENCODER).protocol(LiveProtocol.WHIP).status(status).chatEnabled(true)
                .livekitRoom("live-room").ingressId("ing1").ingressUrl("https://whip").ingressKey("secret-key")
                .createdAt(LocalDateTime.now()).build();
        when(repo.findById(s.getId())).thenReturn(Optional.of(s));
        return s;
    }

    private static CreateLiveRequest request(LiveSource source, String title) {
        return new CreateLiveRequest(title, "mô tả", source, null, true, null, null);
    }

    private static HttpStatus statusOf(Throwable t) {
        return HttpStatus.valueOf(((ResponseStatusException) t).getStatusCode().value());
    }

    @BeforeEach
    void setUp() {
        service = new LiveStreamService(repo, userRepo, channelRepo, gateway, rateLimiter, publisher,
                mock(NotificationService.class), polls, cloudinary, new LiveRecordingProperties(),
                mock(TopicCatalog.class), mock(VideoService.class), List.of(cleanup), tm);
        when(repo.save(any(LiveStream.class))).thenAnswer(inv -> {
            LiveStream s = inv.getArgument(0);
            if (s.getId() == null) {
                s.setId(UUID.randomUUID());
            }
            return s;
        });
        when(repo.findFirstByChannelIdAndStatusIn(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    void createChecksAndSavesInsideItsOwnCommittedTransactionSoTheChannelLockCoversTheCommit() {
        givenOwnerWithChannel(ActiveStatus.ACTIVE);
        boolean[] checkedInside = new boolean[1];
        boolean[] savedInside = new boolean[1];
        when(repo.findFirstByChannelIdAndStatusIn(any(), any())).thenAnswer(inv -> {
            checkedInside[0] = tm.insideRequiresNew();
            return Optional.empty();
        });
        when(repo.save(any(LiveStream.class))).thenAnswer(inv -> {
            savedInside[0] = tm.insideRequiresNew();
            LiveStream s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        service.create(owner, request(LiveSource.BROWSER, "Tiêu đề"));

        // Nếu check + INSERT chạy trong transaction ngoài thì commit xảy ra sau khi nhả khoá, hai yêu cầu cùng lọt qua
        assertThat(checkedInside[0]).isTrue();
        assertThat(savedInside[0]).isTrue();
        assertThat(tm.active()).isZero();
        assertThat(tm.committed).isEqualTo(1);
    }

    @Test
    void theLivekitRoomHasAHardParticipantCapAboveTheViewerCap() {
        givenOwnerWithChannel(ActiveStatus.ACTIVE);

        service.create(owner, request(LiveSource.BROWSER, "Tiêu đề"));

        // 40 người xem + chủ kênh + chỗ dự phòng: token xin dồn cũng không vào quá mức này
        verify(gateway).createRoom(any(), any(), eq(LiveStreamService.VIEWER_CAP + 5));
    }

    @Test
    void createRejectsSecondUnfinishedStreamWith409AndExistingId() {
        givenOwnerWithChannel(ActiveStatus.ACTIVE);
        LiveStream existing = stored(LiveStatus.LIVE);
        when(repo.findFirstByChannelIdAndStatusIn(eq(channelId), any())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.create(owner, request(LiveSource.BROWSER, "Tiêu đề")))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(statusOf(e)).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getReason()).contains(existing.getId().toString());
                });
        verifyNoInteractions(gateway);
    }

    @Test
    void createBlockedForHiddenChannel403() {
        givenOwnerWithChannel(ActiveStatus.INACTIVE);

        assertThatThrownBy(() -> service.create(owner, request(LiveSource.BROWSER, "Tiêu đề")))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.FORBIDDEN));
        verify(repo, never()).save(any());
    }

    @Test
    void createWithBrowserHasNoIngressAndNoProtocol() {
        givenOwnerWithChannel(ActiveStatus.ACTIVE);

        LiveView view = service.create(owner, request(LiveSource.BROWSER, "Tiêu đề"));

        assertThat(view.status()).isEqualTo(LiveStatus.CREATED);
        assertThat(view.protocol()).isNull();
        assertThat(view.ingress()).isNull();
        verify(gateway, never()).createIngress(any(), any(), any(), any());
        ArgumentCaptor<LiveStream> saved = ArgumentCaptor.forClass(LiveStream.class);
        verify(repo).save(saved.capture());
        assertThat(saved.getValue().getLivekitRoom()).startsWith("live-");
        verify(gateway).createRoom(eq(saved.getValue().getLivekitRoom()), any(), eq(45));
    }

    @Test
    void createWithEncoderReturnsIngressAndSavesCreated() {
        givenOwnerWithChannel(ActiveStatus.ACTIVE);
        when(gateway.createIngress(any(), any(), any(), any())).thenReturn(new IngressDetails("ing1", "https://whip", "key1"));

        LiveView view = service.create(owner, request(LiveSource.ENCODER, "Tiêu đề"));

        assertThat(view.ingress().streamKey()).isEqualTo("key1");
        assertThat(view.ingress().url()).isEqualTo("https://whip");
        ArgumentCaptor<LiveStream> saved = ArgumentCaptor.forClass(LiveStream.class);
        verify(repo).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(LiveStatus.CREATED);
        assertThat(saved.getValue().getProtocol()).isEqualTo(LiveProtocol.WHIP);
        assertThat(saved.getValue().getIngressId()).isEqualTo("ing1");
        verify(gateway).createIngress(eq(saved.getValue().getLivekitRoom()), eq("u:" + owner), any(), eq(LiveProtocol.WHIP));
    }

    @Test
    void createWhenGatewayThrowsQuotaReturns503WithMonthMessageAndSavesNothing() {
        givenOwnerWithChannel(ActiveStatus.ACTIVE);
        when(gateway.createIngress(any(), any(), any(), any()))
                .thenThrow(new LiveKitUnavailableException("quota", true));

        assertThatThrownBy(() -> service.create(owner, request(LiveSource.ENCODER, "Tiêu đề")))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(statusOf(e)).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(e.getReason()).contains("hạn mức");
                });
        verify(repo, never()).save(any());
        verify(gateway).deleteRoom(argThat(room -> room.startsWith("live-")));
    }

    @Test
    void createWhenRoomCreationFailsWithoutQuotaIsGenericUnavailable() {
        givenOwnerWithChannel(ActiveStatus.ACTIVE);
        doThrow(new LiveKitUnavailableException("down", false)).when(gateway).createRoom(any(), any(), anyInt());

        assertThatThrownBy(() -> service.create(owner, request(LiveSource.BROWSER, "Tiêu đề")))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(statusOf(e)).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(e.getReason()).doesNotContain("hạn mức");
                });
        verify(repo, never()).save(any());
    }

    @Test
    void titleLongerThan100OrBlankRejected400() {
        givenOwnerWithChannel(ActiveStatus.ACTIVE);

        assertThatThrownBy(() -> service.create(owner, request(LiveSource.BROWSER, "x".repeat(101))))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.create(owner, request(LiveSource.BROWSER, "   ")))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(gateway);
    }

    @Test
    void createWithoutChannelIs403() {
        AppUser noChannel = AppUser.builder().id(owner).email("a@b").name("A").build();
        when(userRepo.findById(owner)).thenReturn(Optional.of(noChannel));

        assertThatThrownBy(() -> service.create(owner, request(LiveSource.BROWSER, "Tiêu đề")))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void updateByNonOwnerIs403() {
        LiveStream s = stored(LiveStatus.LIVE);

        assertThatThrownBy(() -> service.update(UUID.randomUUID(), s.getId(), new UpdateLiveRequest("Mới", null, null)))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.FORBIDDEN));
        verify(repo, never()).save(any());
    }

    @Test
    void updateChatEnabledBroadcastsChatState() {
        givenOwnerWithChannel(ActiveStatus.ACTIVE);
        LiveStream s = stored(LiveStatus.LIVE);

        service.update(owner, s.getId(), new UpdateLiveRequest(null, null, false));

        assertThat(s.isChatEnabled()).isFalse();
        ArgumentCaptor<LiveEvent> event = ArgumentCaptor.forClass(LiveEvent.class);
        verify(publisher).toStream(eq(s.getId()), event.capture());
        assertThat(event.getValue().getType()).isEqualTo("CHAT_STATE");
        assertThat(event.getValue().getChatEnabled()).isFalse();
    }

    @Test
    void updateEndedStreamIs409() {
        LiveStream s = stored(LiveStatus.ENDED);

        assertThatThrownBy(() -> service.update(owner, s.getId(), new UpdateLiveRequest("Mới", null, null)))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void endByNonOwnerIs403ButAdminCanEnd() {
        givenOwnerWithChannel(ActiveStatus.ACTIVE);
        LiveStream s = stored(LiveStatus.LIVE);
        UUID stranger = UUID.randomUUID();

        assertThatThrownBy(() -> service.end(stranger, false, s.getId()))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.FORBIDDEN));
        assertThat(s.getStatus()).isEqualTo(LiveStatus.LIVE);

        service.end(stranger, true, s.getId());

        assertThat(s.getStatus()).isEqualTo(LiveStatus.ENDED);
        assertThat(s.getEndedAt()).isNotNull();
    }

    @Test
    void endDeletesRoomAndIngressRunsCleanupAndBroadcastsEnded() {
        givenOwnerWithChannel(ActiveStatus.ACTIVE);
        LiveStream s = stored(LiveStatus.LIVE);
        s.setCurrentViewers(7);

        service.end(owner, false, s.getId());

        verify(gateway).deleteRoom("live-room");
        verify(gateway).deleteIngress("ing1");
        verify(cleanup).onEnded(s.getId());
        assertThat(s.getCurrentViewers()).isZero();
        ArgumentCaptor<LiveEvent> event = ArgumentCaptor.forClass(LiveEvent.class);
        verify(publisher).toStream(eq(s.getId()), event.capture());
        assertThat(event.getValue().getType()).isEqualTo("STATUS");
        assertThat(event.getValue().getStatus()).isEqualTo("ENDED");
    }

    @Test
    void endStillEndsWhenLivekitIsDown() {
        givenOwnerWithChannel(ActiveStatus.ACTIVE);
        LiveStream s = stored(LiveStatus.LIVE);
        doThrow(new LiveKitUnavailableException("down", false)).when(gateway).deleteRoom(any());
        doThrow(new LiveKitUnavailableException("down", false)).when(gateway).deleteIngress(any());

        service.end(owner, false, s.getId());

        assertThat(s.getStatus()).isEqualTo(LiveStatus.ENDED);
        verify(cleanup).onEnded(s.getId());
    }

    @Test
    void endIsIdempotent() {
        givenOwnerWithChannel(ActiveStatus.ACTIVE);
        LiveStream s = stored(LiveStatus.ENDED);

        service.end(owner, false, s.getId());

        verifyNoInteractions(gateway);
        verify(cleanup, never()).onEnded(any());
    }

    @Test
    void viewForNonOwnerHidesIngressButOwnerSeesIt() {
        givenOwnerWithChannel(ActiveStatus.ACTIVE);
        LiveStream s = stored(LiveStatus.CREATED);

        assertThat(service.get(s.getId(), UUID.randomUUID()).ingress()).isNull();
        assertThat(service.get(s.getId(), null).ingress()).isNull();
        assertThat(service.get(s.getId(), owner).ingress().streamKey()).isEqualTo("secret-key");
    }

    @Test
    void liveViewCarriesTheViewersCurrentPollButNoHistory() {
        givenOwnerWithChannel(ActiveStatus.ACTIVE);
        LiveStream s = stored(LiveStatus.LIVE);
        UUID viewer = UUID.randomUUID();
        PollView poll = new PollView("p1", "Chơi gì?", List.of("a", "b"), List.of(1, 0), 1, true, 0);
        when(polls.current(s.getId(), viewer)).thenReturn(Optional.of(poll));

        LiveView view = service.get(s.getId(), viewer);

        assertThat(view.poll()).isSameAs(poll);
        assertThat(view.polls()).isNull();
    }

    @Test
    void endedViewListsEveryPollWithFinalCountsAndNoCurrentPoll() {
        givenOwnerWithChannel(ActiveStatus.ACTIVE);
        LiveStream s = stored(LiveStatus.ENDED);
        List<PollView> history = List.of(
                new PollView("p1", "Một?", List.of("a", "b"), List.of(3, 2), 5, false, null),
                new PollView("p2", "Hai?", List.of("x", "y"), List.of(0, 9), 9, false, null));
        when(polls.history(s.getId())).thenReturn(history);

        LiveView view = service.get(s.getId(), null);

        assertThat(view.polls()).isEqualTo(history);
        assertThat(view.poll()).isNull();
    }

    @Test
    void canceledStreamIs404() {
        LiveStream s = stored(LiveStatus.CANCELED);

        assertThatThrownBy(() -> service.get(s.getId(), null))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void listLiveKeepsRepositoryOrderAndJoinsChannelInfo() {
        givenOwnerWithChannel(ActiveStatus.ACTIVE);
        LiveStream popular = stored(LiveStatus.LIVE);
        popular.setCurrentViewers(30);
        LiveStream quiet = stored(LiveStatus.LIVE);
        quiet.setCurrentViewers(2);
        when(repo.findByStatusOrderByCurrentViewersDesc(LiveStatus.LIVE)).thenReturn(List.of(popular, quiet));

        List<LiveCard> cards = service.listLive();

        assertThat(cards).extracting(LiveCard::viewers).containsExactly(30, 2);
        assertThat(cards.get(0).channelName()).isEqualTo("Kênh Sơn");
        assertThat(cards.get(0).channelBackground()).isEqualTo("banner.png");
    }

    private static MockMultipartFile image(String type, int bytes) {
        return new MockMultipartFile("file", "a", type, new byte[bytes]);
    }

    @Test
    void thumbnailIsStoredShownOnViewAndCardsAndReplacesTheOldOne() {
        givenOwnerWithChannel(ActiveStatus.ACTIVE);
        LiveStream s = stored(LiveStatus.CREATED);
        s.setThumbnailUrl("https://res.cloudinary.com/x/image/upload/old.jpg");
        MockMultipartFile file = image("image/png", 100);
        when(cloudinary.uploadThumbnail(file, false)).thenReturn("https://res.cloudinary.com/x/image/upload/new.jpg");

        LiveView view = service.uploadThumbnail(owner, s.getId(), file);

        assertThat(view.thumbnailUrl()).isEqualTo("https://res.cloudinary.com/x/image/upload/new.jpg");
        assertThat(s.getThumbnailUrl()).isEqualTo("https://res.cloudinary.com/x/image/upload/new.jpg");
        verify(repo).save(s);
        verify(cloudinary).deleteImage("https://res.cloudinary.com/x/image/upload/old.jpg");
        verify(rateLimiter).check(Limit.LIVE_THUMBNAIL, owner);

        ArgumentCaptor<LiveEvent> event = ArgumentCaptor.forClass(LiveEvent.class);
        verify(publisher).toStream(eq(s.getId()), event.capture());
        assertThat(event.getValue().getType()).isEqualTo("THUMBNAIL");
        assertThat(event.getValue().getThumbnailUrl()).isEqualTo("https://res.cloudinary.com/x/image/upload/new.jpg");

        when(repo.findByStatusOrderByCurrentViewersDesc(LiveStatus.LIVE)).thenReturn(List.of(s));
        assertThat(service.listLive().get(0).thumbnailUrl()).isEqualTo("https://res.cloudinary.com/x/image/upload/new.jpg");
    }

    @Test
    void firstThumbnailDeletesNothing() {
        givenOwnerWithChannel(ActiveStatus.ACTIVE);
        LiveStream s = stored(LiveStatus.LIVE);
        MockMultipartFile file = image("image/jpeg", 100);
        when(cloudinary.uploadThumbnail(file, false)).thenReturn("https://res.cloudinary.com/x/image/upload/new.jpg");

        service.uploadThumbnail(owner, s.getId(), file);

        verify(cloudinary, never()).deleteImage(any());
    }

    @Test
    void thumbnailFromAnotherUserIsForbiddenAndNothingIsUploaded() {
        givenOwnerWithChannel(ActiveStatus.ACTIVE);
        LiveStream s = stored(LiveStatus.LIVE);

        assertThatThrownBy(() -> service.uploadThumbnail(UUID.randomUUID(), s.getId(), image("image/png", 10)))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.FORBIDDEN));
        verifyNoInteractions(cloudinary);
    }

    @Test
    void thumbnailOfFinishedStreamIsConflict() {
        givenOwnerWithChannel(ActiveStatus.ACTIVE);
        LiveStream ended = stored(LiveStatus.ENDED);

        assertThatThrownBy(() -> service.uploadThumbnail(owner, ended.getId(), image("image/png", 10)))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.CONFLICT));
        verifyNoInteractions(cloudinary);
    }

    @Test
    void thumbnailMustBePngOrJpegUnderTwoMegabytes() {
        givenOwnerWithChannel(ActiveStatus.ACTIVE);
        LiveStream s = stored(LiveStatus.LIVE);

        for (MockMultipartFile bad : List.of(image("image/gif", 10), image("text/plain", 10),
                image("image/png", 2 * 1024 * 1024 + 1), image("image/png", 0))) {
            assertThatThrownBy(() -> service.uploadThumbnail(owner, s.getId(), bad))
                    .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.BAD_REQUEST));
        }
        assertThatThrownBy(() -> service.uploadThumbnail(owner, s.getId(), null))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(cloudinary);
        assertThat(s.getThumbnailUrl()).isNull();
    }

    @Test
    void thumbnailOfexactlyTwoMegabytesIsAccepted() {
        givenOwnerWithChannel(ActiveStatus.ACTIVE);
        LiveStream s = stored(LiveStatus.LIVE);
        MockMultipartFile file = image("image/png", 2 * 1024 * 1024);
        when(cloudinary.uploadThumbnail(file, false)).thenReturn("https://res.cloudinary.com/x/image/upload/ok.jpg");

        assertThat(service.uploadThumbnail(owner, s.getId(), file).thumbnailUrl()).endsWith("ok.jpg");
    }

    @Test
    void mineReturnsUnfinishedStreamWithIngress() {
        givenOwnerWithChannel(ActiveStatus.ACTIVE);
        LiveStream s = stored(LiveStatus.CREATED);
        when(repo.findFirstByChannelIdAndStatusIn(eq(channelId), any())).thenReturn(Optional.of(s));

        assertThat(service.mine(owner)).isPresent();
        assertThat(service.mine(owner).get().ingress().url()).isEqualTo("https://whip");

        when(repo.findFirstByChannelIdAndStatusIn(eq(channelId), any())).thenReturn(Optional.empty());
        assertThat(service.mine(owner)).isEmpty();
    }
}
