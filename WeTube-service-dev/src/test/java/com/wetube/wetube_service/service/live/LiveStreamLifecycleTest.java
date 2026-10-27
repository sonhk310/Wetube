package com.wetube.wetube_service.service.live;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.wetube.wetube_service.dto.live.LiveEvent;
import com.wetube.wetube_service.dto.live.LiveTokenResponse;
import com.wetube.wetube_service.entity.channel.Channel;
import com.wetube.wetube_service.entity.live.LiveStream;
import com.wetube.wetube_service.enumeration.LiveSource;
import com.wetube.wetube_service.enumeration.LiveStatus;
import com.wetube.wetube_service.repository.UserRepository;
import com.wetube.wetube_service.repository.channel.ChannelRepository;
import com.wetube.wetube_service.repository.live.LiveStreamRepository;
import com.wetube.wetube_service.security.RateLimiter;
import com.wetube.wetube_service.service.live.LiveKitGateway.Participant;
import com.wetube.wetube_service.service.notification.NotificationService;

/** Chuyển trạng thái theo LiveKit và theo thời gian, cấp token xem / phát. Giờ được truyền vào nên test không chờ. */
class LiveStreamLifecycleTest {

    private final LiveStreamRepository repo = mock(LiveStreamRepository.class);
    private final ChannelRepository channelRepo = mock(ChannelRepository.class);
    private final LiveKitGateway gateway = mock(LiveKitGateway.class);
    private final LiveEventPublisher publisher = mock(LiveEventPublisher.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final LiveCleanup cleanup = mock(LiveCleanup.class);

    private final UUID owner = UUID.randomUUID();
    private final UUID channelId = UUID.randomUUID();
    private final LocalDateTime now = LocalDateTime.of(2026, 10, 27, 12, 0, 0);
    private final List<LiveStream> unfinished = new ArrayList<>();
    private LiveStreamService service;

    @BeforeEach
    void setUp() {
        service = new LiveStreamService(repo, mock(UserRepository.class), channelRepo, gateway,
                mock(RateLimiter.class), publisher, notifications, List.of(cleanup));
        when(repo.findByStatusIn(any())).thenAnswer(inv -> List.copyOf(unfinished));
        when(repo.save(any(LiveStream.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gateway.wsUrl()).thenReturn("wss://livekit.test");
        when(channelRepo.findById(channelId)).thenReturn(Optional.of(
                Channel.builder().id(channelId).name("Kênh Sơn").build()));
    }

    private LiveStream stream(LiveStatus status, LiveSource source, LocalDateTime createdAt, LocalDateTime startedAt,
                              LocalDateTime lastPublisherAt) {
        LiveStream s = LiveStream.builder().id(UUID.randomUUID()).channelId(channelId).ownerId(owner)
                .title("Buổi phát").source(source).status(status).chatEnabled(true)
                .livekitRoom("live-" + UUID.randomUUID()).createdAt(createdAt).startedAt(startedAt)
                .lastPublisherAt(lastPublisherAt).build();
        when(repo.findById(s.getId())).thenReturn(Optional.of(s));
        unfinished.add(s);
        return s;
    }

    private LiveStream liveStream(LocalDateTime lastPublisherAt) {
        return stream(LiveStatus.LIVE, LiveSource.BROWSER, now.minusHours(1), now.minusMinutes(50), lastPublisherAt);
    }

    private LiveStream createdStream() {
        return stream(LiveStatus.CREATED, LiveSource.BROWSER, now.minusMinutes(5), null, null);
    }

    private Participant publisher() {
        return new Participant("u:" + owner, true);
    }

    private static List<Participant> viewers(int n) {
        List<Participant> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(new Participant("v:" + i, false));
        }
        return list;
    }

    private static List<Participant> withPublisher(Participant pub, int viewerCount) {
        List<Participant> list = new ArrayList<>(viewers(viewerCount));
        list.add(pub);
        return list;
    }

    private static HttpStatus statusOf(Throwable t) {
        return HttpStatus.valueOf(((ResponseStatusException) t).getStatusCode().value());
    }

    private List<LiveEvent> eventsOf(LiveStream s) {
        ArgumentCaptor<LiveEvent> captor = ArgumentCaptor.forClass(LiveEvent.class);
        verify(publisher, org.mockito.Mockito.atLeast(0)).toStream(eq(s.getId()), captor.capture());
        return captor.getAllValues();
    }

    // ------------------------------------------------------------ vòng đời

    @Test
    void createdBecomesLiveWhenPublisherHasVideoAndNotifiesOnce() {
        LiveStream s = createdStream();
        when(gateway.participants(s.getLivekitRoom())).thenReturn(withPublisher(publisher(), 1));

        service.refreshAll(now);
        service.refreshAll(now.plusSeconds(15));

        assertThat(s.getStatus()).isEqualTo(LiveStatus.LIVE);
        assertThat(s.getStartedAt()).isEqualTo(now);
        assertThat(s.getLastPublisherAt()).isEqualTo(now.plusSeconds(15));
        verify(notifications, times(1)).notifyLiveStarted(s.getId(), owner, channelId, "Buổi phát");
        assertThat(eventsOf(s)).anySatisfy(e -> {
            assertThat(e.getType()).isEqualTo("STATUS");
            assertThat(e.getStatus()).isEqualTo("LIVE");
        });
    }

    @Test
    void createdStaysCreatedWhilePublisherHasNoVideoYet() {
        LiveStream s = createdStream();
        when(gateway.participants(s.getLivekitRoom())).thenReturn(List.of(new Participant("u:" + owner, false)));

        service.refreshAll(now);

        assertThat(s.getStatus()).isEqualTo(LiveStatus.CREATED);
        verify(notifications, never()).notifyLiveStarted(any(), any(), any(), any());
    }

    @Test
    void liveEndsAfterTwoMinutesWithoutPublisher() {
        LiveStream s = liveStream(now.minusSeconds(119));
        when(gateway.participants(s.getLivekitRoom())).thenReturn(viewers(2));

        service.refreshAll(now);
        assertThat(s.getStatus()).isEqualTo(LiveStatus.LIVE);

        s.setLastPublisherAt(now.minusSeconds(121));
        service.refreshAll(now);

        assertThat(s.getStatus()).isEqualTo(LiveStatus.ENDED);
        assertThat(s.getEndedAt()).isEqualTo(now);
        verify(gateway).deleteRoom(s.getLivekitRoom());
        verify(cleanup).onEnded(s.getId());
    }

    @Test
    void roomMissingFromLivekitCountsAsNoPublisher() {
        // Mô phỏng backend deploy lại / LiveKit dọn phòng: không còn ai trong phòng
        LiveStream s = liveStream(now.minusMinutes(3));
        when(gateway.participants(s.getLivekitRoom())).thenReturn(List.of());

        service.refreshAll(now);

        assertThat(s.getStatus()).isEqualTo(LiveStatus.ENDED);
    }

    @Test
    void livekitDownDoesNotEndALiveStream() {
        LiveStream s = liveStream(now.minusMinutes(10));
        when(gateway.participants(s.getLivekitRoom())).thenThrow(new LiveKitUnavailableException("down", false));

        service.refreshAll(now);

        assertThat(s.getStatus()).isEqualTo(LiveStatus.LIVE);
        verify(gateway, never()).deleteRoom(any());
    }

    @Test
    void createdIsCanceledAfterOneHour() {
        LiveStream young = stream(LiveStatus.CREATED, LiveSource.BROWSER, now.minusMinutes(59), null, null);
        LiveStream old = stream(LiveStatus.CREATED, LiveSource.BROWSER, now.minusMinutes(61), null, null);
        when(gateway.participants(any())).thenReturn(List.of());

        service.refreshAll(now);

        assertThat(young.getStatus()).isEqualTo(LiveStatus.CREATED);
        assertThat(old.getStatus()).isEqualTo(LiveStatus.CANCELED);
        verify(gateway).deleteRoom(old.getLivekitRoom());
    }

    @Test
    void liveEndsAfterTwelveHoursEvenWithPublisher() {
        LiveStream s = stream(LiveStatus.LIVE, LiveSource.BROWSER, now.minusHours(13),
                now.minusHours(12).minusMinutes(1), now);
        when(gateway.participants(s.getLivekitRoom())).thenReturn(withPublisher(publisher(), 0));

        service.refreshAll(now);

        assertThat(s.getStatus()).isEqualTo(LiveStatus.ENDED);
    }

    @Test
    void peakViewersOnlyGrows() {
        LiveStream s = liveStream(now);
        s.setPeakViewers(5);
        when(gateway.participants(s.getLivekitRoom())).thenReturn(withPublisher(publisher(), 3));

        service.refreshAll(now);
        assertThat(s.getCurrentViewers()).isEqualTo(3);
        assertThat(s.getPeakViewers()).isEqualTo(5);

        when(gateway.participants(s.getLivekitRoom())).thenReturn(withPublisher(publisher(), 8));
        service.refreshAll(now.plusSeconds(15));
        assertThat(s.getCurrentViewers()).isEqualTo(8);
        assertThat(s.getPeakViewers()).isEqualTo(8);
    }

    @Test
    void viewersEventIsSentOnlyWhenTheCountChanges() {
        LiveStream s = liveStream(now);
        s.setCurrentViewers(3);
        when(gateway.participants(s.getLivekitRoom())).thenReturn(withPublisher(publisher(), 3));
        service.refreshAll(now);
        assertThat(eventsOf(s)).noneMatch(e -> "VIEWERS".equals(e.getType()));

        when(gateway.participants(s.getLivekitRoom())).thenReturn(withPublisher(publisher(), 4));
        service.refreshAll(now.plusSeconds(15));

        assertThat(eventsOf(s)).filteredOn(e -> "VIEWERS".equals(e.getType()))
                .extracting(LiveEvent::getViewers).containsExactly(4);
    }

    @Test
    void oneStreamFailingDoesNotStopOthers() {
        LiveStream broken = liveStream(now);
        LiveStream fine = liveStream(now);
        when(gateway.participants(broken.getLivekitRoom())).thenThrow(new RuntimeException("boom"));
        when(gateway.participants(fine.getLivekitRoom())).thenReturn(withPublisher(publisher(), 2));

        service.refreshAll(now);

        assertThat(fine.getCurrentViewers()).isEqualTo(2);
    }

    // ----------------------------------------------------------- markStarted

    @Test
    void markStartedByNonOwnerIs403() {
        LiveStream s = createdStream();

        assertThatThrownBy(() -> service.markStarted(UUID.randomUUID(), s.getId()))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void markStartedPromotesOnlyWhenLivekitSeesVideo() {
        LiveStream s = createdStream();
        when(gateway.participants(s.getLivekitRoom())).thenReturn(List.of(new Participant("u:" + owner, false)));
        service.markStarted(owner, s.getId());
        assertThat(s.getStatus()).isEqualTo(LiveStatus.CREATED);

        when(gateway.participants(s.getLivekitRoom())).thenReturn(withPublisher(publisher(), 0));
        service.markStarted(owner, s.getId());

        assertThat(s.getStatus()).isEqualTo(LiveStatus.LIVE);
        verify(notifications).notifyLiveStarted(s.getId(), owner, channelId, "Buổi phát");
    }

    // ----------------------------------------------------------------- token

    @Test
    void tokenForViewerHasNoPublishRightAndAnonymousIdentity() {
        LiveStream s = liveStream(now);
        when(gateway.participants(s.getLivekitRoom())).thenReturn(withPublisher(publisher(), 2));
        when(gateway.viewerToken(eq(s.getLivekitRoom()), argThat(id -> id.startsWith("v:")))).thenReturn("VIEW");

        LiveTokenResponse res = service.token(s.getId(), UUID.randomUUID());

        assertThat(res.token()).isEqualTo("VIEW");
        assertThat(res.role()).isEqualTo("VIEWER");
        assertThat(res.full()).isFalse();
        assertThat(res.wsUrl()).isEqualTo("wss://livekit.test");
        verify(gateway, never()).publisherToken(any(), any(), any());
    }

    @Test
    void ownerWebcamGetsPublisherToken() {
        LiveStream s = createdStream();
        when(gateway.publisherToken(eq(s.getLivekitRoom()), eq("u:" + owner), any())).thenReturn("PUB");

        LiveTokenResponse res = service.token(s.getId(), owner);

        assertThat(res.token()).isEqualTo("PUB");
        assertThat(res.role()).isEqualTo("PUBLISHER");
    }

    @Test
    void encoderOwnerGetsViewerTokenBecauseObsPublishes() {
        LiveStream s = stream(LiveStatus.CREATED, LiveSource.ENCODER, now.minusMinutes(1), null, null);
        when(gateway.participants(s.getLivekitRoom())).thenReturn(List.of());
        when(gateway.viewerToken(eq(s.getLivekitRoom()), any())).thenReturn("VIEW");

        LiveTokenResponse res = service.token(s.getId(), owner);

        assertThat(res.role()).isEqualTo("VIEWER");
        verify(gateway, never()).publisherToken(any(), any(), any());
    }

    @Test
    void fortyFirstViewerGetsFullAndNoToken() {
        LiveStream s = liveStream(now);
        when(gateway.viewerToken(any(), any())).thenReturn("VIEW");

        when(gateway.participants(s.getLivekitRoom())).thenReturn(withPublisher(publisher(), 39));
        assertThat(service.token(s.getId(), null).token()).isEqualTo("VIEW");

        when(gateway.participants(s.getLivekitRoom())).thenReturn(withPublisher(publisher(), 40));
        LiveTokenResponse full = service.token(s.getId(), null);
        assertThat(full.full()).isTrue();
        assertThat(full.token()).isNull();
    }

    @Test
    void tokenForEndedStreamIs410AndCanceledIs404() {
        LiveStream ended = stream(LiveStatus.ENDED, LiveSource.BROWSER, now.minusHours(2), now.minusHours(1), now);
        LiveStream canceled = stream(LiveStatus.CANCELED, LiveSource.BROWSER, now.minusHours(2), null, null);

        assertThatThrownBy(() -> service.token(ended.getId(), null))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.GONE));
        assertThatThrownBy(() -> service.token(canceled.getId(), null))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void livekitFailureWhileIssuingTokenIs503() {
        LiveStream s = liveStream(now);
        when(gateway.participants(s.getLivekitRoom())).thenThrow(new LiveKitUnavailableException("quota", true));

        assertThatThrownBy(() -> service.token(s.getId(), null))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(statusOf(e)).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(e.getReason()).contains("hạn mức");
                });
    }

    // --------------------------------------------------------------- Ingress

    @Test
    void cleanInactiveIngressKeepsEveryUnfinishedStreamsIngress() {
        LiveStream a = createdStream();
        a.setIngressId("ing-a");
        LiveStream b = liveStream(now);
        b.setIngressId("ing-b");
        liveStream(now); // không có Ingress
        when(gateway.deleteInactiveIngress(any())).thenReturn(2);

        int deleted = service.cleanInactiveIngress();

        assertThat(deleted).isEqualTo(2);
        verify(gateway, atLeastOnce()).deleteInactiveIngress(argThat(keep ->
                keep.size() == 2 && keep.containsAll(Set.of("ing-a", "ing-b"))));
    }
}
