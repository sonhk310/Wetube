package com.wetube.wetube_service.service.watchRoom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.wetube.wetube_service.dto.room.RoomEvent;
import com.wetube.wetube_service.dto.room.RoomIn;
import com.wetube.wetube_service.dto.room.VideoRoom;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.repository.UserRepository;
import com.wetube.wetube_service.security.RateLimiter;
import com.wetube.wetube_service.service.RedisService;

import java.util.Map;

@ExtendWith(MockitoExtension.class)
class WatchRoomServiceTest {

    @Mock RedisService redis;
    @Mock RateLimiter rateLimiter;
    @Mock SimpMessagingTemplate messaging;
    @Mock UserRepository userRepo;
    @Mock RoomVideoLookup videos;

    WatchRoomService service;

    final UUID hostId = UUID.randomUUID();
    final UUID guestId = UUID.randomUUID();
    final UUID thirdId = UUID.randomUUID();
    /** Lưu "Redis" trong bộ nhớ để thử khôi phục phòng sau khi backend khởi động lại. */
    final Map<String, String> fakeRedis = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() {
        lenient().when(rateLimiter.tryAcquire(any(), any())).thenReturn(true);
        lenient().when(redis.get(anyString())).thenAnswer(i -> fakeRedis.get(i.<String>getArgument(0)));
        lenient().doAnswer(i -> fakeRedis.put(i.getArgument(0), i.getArgument(1)))
                .when(redis).set(anyString(), anyString(), any());
        lenient().doAnswer(i -> fakeRedis.remove(i.<String>getArgument(0))).when(redis).delete(anyString());
        for (UUID id : List.of(hostId, guestId, thirdId)) {
            lenient().when(userRepo.findById(id))
                    .thenReturn(Optional.of(AppUser.builder().id(id).name("User " + id.toString().substring(0, 4)).build()));
        }
        lenient().when(videos.find("v1")).thenReturn(video("v1"));
        lenient().when(videos.find("v2")).thenReturn(video("v2"));
        service = newService();
    }

    private WatchRoomService newService() {
        return new WatchRoomService(redis, rateLimiter, messaging, userRepo, videos);
    }

    private static VideoRoom video(String id) {
        VideoRoom v = new VideoRoom();
        v.setId(id);
        v.setTitle("Video " + id);
        return v;
    }

    private static RoomIn.Control control(String type, long version, double position) {
        RoomIn.Control c = new RoomIn.Control();
        c.setType(type);
        c.setVersion(version);
        c.setPosition(position);
        return c;
    }

    private static RoomIn.Control change(long version, String videoId) {
        RoomIn.Control c = control("CHANGE", version, 0);
        c.setVideoId(videoId);
        return c;
    }

    /** Sự kiện đã gửi riêng cho một người, theo thứ tự. */
    private List<RoomEvent> sentTo(String userId) {
        ArgumentCaptor<Object> events = ArgumentCaptor.forClass(Object.class);
        org.mockito.Mockito.verify(messaging, org.mockito.Mockito.atLeast(0))
                .convertAndSendToUser(org.mockito.ArgumentMatchers.eq(userId), anyString(), events.capture());
        return new ArrayList<RoomEvent>(events.getAllValues().stream().map(RoomEvent.class::cast).toList());
    }

    private RoomEvent lastSnapshot(String userId) {
        List<RoomEvent> sent = sentTo(userId);
        return sent.get(sent.size() - 1);
    }

    private String roomWithHostAndGuest() {
        String code = service.create(hostId, "v1", 30.0);
        service.join(code, "s-host", hostId.toString());
        service.join(code, "s-guest", guestId.toString());
        return code;
    }

    @Test
    void createStartsAtGivenVideoAndPosition() {
        String code = service.create(hostId, "v1", 30.0);

        assertThat(code).matches("[A-Za-z0-9]{3}-[A-Za-z0-9]{4}-[A-Za-z0-9]{3}");
        service.join(code, "s-host", hostId.toString());
        RoomEvent snapshot = lastSnapshot(hostId.toString());
        assertThat(snapshot.getType()).isEqualTo("SNAPSHOT");
        assertThat(snapshot.getState().getVideoId()).isEqualTo("v1");
        assertThat(snapshot.getState().getPosition()).isEqualTo(30.0);
        assertThat(snapshot.getHostUserId()).isEqualTo(hostId.toString());
        assertThat(snapshot.getMembers()).hasSize(1);
    }

    @Test
    void anyMemberCanControlAndVersionAdvances() {
        String code = roomWithHostAndGuest();
        long version = lastSnapshot(guestId.toString()).getState().getVersion();

        service.control(code, guestId.toString(), control("PAUSE", version, 42));

        RoomEvent snapshot = joinAgain(code, "s-third", thirdId);
        assertThat(snapshot.getState().isPlaying()).isFalse();
        assertThat(snapshot.getState().getPosition()).isEqualTo(42);
        assertThat(snapshot.getState().getVersion()).isEqualTo(version + 1);
        assertThat(snapshot.getState().getLastActor()).isEqualTo(guestId.toString());
    }

    private RoomEvent joinAgain(String code, String session, UUID user) {
        service.join(code, session, user.toString());
        return lastSnapshot(user.toString());
    }

    @Test
    void staleVersionFromAnotherUserIsRejectedAndResynced() {
        String code = roomWithHostAndGuest();
        long version = lastSnapshot(hostId.toString()).getState().getVersion();
        service.control(code, hostId.toString(), control("PAUSE", version, 10));

        // Khách còn thấy phiên bản cũ: thao tác bị bỏ, khách nhận lại trạng thái mới nhất
        service.control(code, guestId.toString(), control("PLAY", version, 99));

        RoomEvent resync = lastSnapshot(guestId.toString());
        assertThat(resync.getType()).isEqualTo("STATE");
        assertThat(resync.getState().isPlaying()).isFalse();
        assertThat(resync.getState().getPosition()).isEqualTo(10);
    }

    @Test
    void sameUserCanChainActionsWithoutWaitingForTheEcho() {
        String code = roomWithHostAndGuest();
        long version = lastSnapshot(hostId.toString()).getState().getVersion();

        service.control(code, hostId.toString(), control("SEEK", version, 100));
        service.control(code, hostId.toString(), control("PAUSE", version, 100)); // vẫn dùng phiên bản cũ

        RoomEvent snapshot = joinAgain(code, "s-third", thirdId);
        assertThat(snapshot.getState().isPlaying()).isFalse();
        assertThat(snapshot.getState().getPosition()).isEqualTo(100);
    }

    @Test
    void onlyTheFirstEndedReportAdvancesTheQueue() {
        String code = roomWithHostAndGuest();
        RoomIn.QueueOp add = new RoomIn.QueueOp();
        add.setOp("ADD");
        add.setVideoId("v2");
        service.queue(code, guestId.toString(), add);
        long version = lastSnapshot(hostId.toString()).getState().getVersion();

        service.control(code, hostId.toString(), control("ENDED", version, 60));
        service.control(code, guestId.toString(), control("ENDED", version, 60)); // báo trùng, phiên bản đã cũ

        RoomEvent snapshot = joinAgain(code, "s-third", thirdId);
        assertThat(snapshot.getState().getVideoId()).isEqualTo("v2");
        assertThat(snapshot.getState().getPosition()).isZero();
        assertThat(snapshot.getQueue()).isEmpty();
    }

    @Test
    void changeVideoRejectsUnavailableVideo() {
        String code = roomWithHostAndGuest();
        long version = lastSnapshot(hostId.toString()).getState().getVersion();

        service.control(code, hostId.toString(), change(version, "private-video"));

        assertThat(lastSnapshot(hostId.toString()).getError()).isEqualTo("INVALID");
        assertThat(joinAgain(code, "s-third", thirdId).getState().getVideoId()).isEqualTo("v1");
    }

    @Test
    void kickedUserCannotJoinAgainAndOnlyHostCanKick() {
        String code = roomWithHostAndGuest();
        RoomIn.Kick kickGuest = new RoomIn.Kick();
        kickGuest.setUserId(guestId.toString());
        RoomIn.Kick kickHost = new RoomIn.Kick();
        kickHost.setUserId(hostId.toString());

        service.kick(code, guestId.toString(), kickHost);  // khách không phải chủ phòng: bị bỏ qua
        service.kick(code, hostId.toString(), kickGuest);
        service.join(code, "s-guest-2", guestId.toString());

        assertThat(lastSnapshot(guestId.toString()).getError()).isEqualTo("KICKED");
        assertThat(joinAgain(code, "s-third", thirdId).getMembers()).hasSize(2); // chủ phòng + người vừa vào
    }

    @Test
    void hostLeavingPassesHostToTheEarliestRemainingMember() {
        String code = roomWithHostAndGuest();

        service.leave(code, "s-host", hostId.toString(), true);

        RoomEvent snapshot = joinAgain(code, "s-third", thirdId);
        assertThat(snapshot.getHostUserId()).isEqualTo(guestId.toString());
        assertThat(snapshot.getMembers()).hasSize(2);
    }

    @Test
    void fullRoomRejectsNewMembers() {
        String code = service.create(hostId, null, null);
        for (int i = 0; i < WatchRoomService.MAX_MEMBERS; i++) {
            UUID id = UUID.randomUUID();
            lenient().when(userRepo.findById(id)).thenReturn(Optional.of(AppUser.builder().id(id).name("U" + i).build()));
            service.join(code, "s" + i, id.toString());
        }

        service.join(code, "s-extra", thirdId.toString());

        assertThat(lastSnapshot(thirdId.toString()).getError()).isEqualTo("FULL");
        assertThat(service.summary(code)).containsEntry("memberCount", WatchRoomService.MAX_MEMBERS).containsEntry("full", true);
    }

    @Test
    void unknownRoomGetsNotFound() {
        service.join("abc-defg-hij", "s1", hostId.toString());

        assertThat(lastSnapshot(hostId.toString()).getError()).isEqualTo("NOT_FOUND");
        assertThat(service.summary("abc-defg-hij")).isNull();
        assertThat(service.summary("not a code!")).isNull();
    }

    @Test
    void roomSurvivesRestartThroughRedis() {
        String code = roomWithHostAndGuest();
        long version = lastSnapshot(hostId.toString()).getState().getVersion();
        service.control(code, hostId.toString(), control("SEEK", version, 77));

        WatchRoomService restarted = newService(); // bộ nhớ trống, Redis còn
        restarted.join(code, "s-host-new", hostId.toString());

        RoomEvent snapshot = lastSnapshot(hostId.toString());
        assertThat(snapshot.getType()).isEqualTo("SNAPSHOT");
        assertThat(snapshot.getState().getVideoId()).isEqualTo("v1");
        assertThat(snapshot.getState().getPosition()).isEqualTo(77);
        assertThat(snapshot.getHostUserId()).isEqualTo(hostId.toString());
        assertThat(snapshot.getChat()).isNotEmpty();
    }

    @Test
    void closeDeletesRoomOnlyForHost() {
        String code = roomWithHostAndGuest();

        service.close(code, guestId.toString());
        assertThat(service.summary(code)).isNotNull();

        service.close(code, hostId.toString());
        assertThat(service.summary(code)).isNull();
        assertThat(fakeRedis).doesNotContainKey("room:" + code);
    }

    @Test
    void formatTimeUsesHoursOnlyWhenNeeded() {
        assertThat(WatchRoomService.formatTime(200)).isEqualTo("3:20");
        assertThat(WatchRoomService.formatTime(3725)).isEqualTo("1:02:05");
    }
}
