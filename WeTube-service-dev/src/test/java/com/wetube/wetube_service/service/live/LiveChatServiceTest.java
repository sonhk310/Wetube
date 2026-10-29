package com.wetube.wetube_service.service.live;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wetube.wetube_service.dto.live.LiveChatMessage;
import com.wetube.wetube_service.dto.live.LiveEvent;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.channel.Channel;
import com.wetube.wetube_service.entity.live.LiveStream;
import com.wetube.wetube_service.enumeration.LiveSource;
import com.wetube.wetube_service.enumeration.LiveStatus;
import com.wetube.wetube_service.repository.UserRepository;
import com.wetube.wetube_service.repository.live.LiveStreamRepository;
import com.wetube.wetube_service.security.RateLimiter;
import com.wetube.wetube_service.security.RateLimiter.Limit;
import com.wetube.wetube_service.service.RedisService;
import com.wetube.wetube_service.service.membership.ChannelMembershipService;
import com.wetube.wetube_service.service.premium.PremiumUserService;

class LiveChatServiceTest {

    private final LiveStreamRepository repo = mock(LiveStreamRepository.class);
    private final UserRepository userRepo = mock(UserRepository.class);
    private final RedisService redis = mock(RedisService.class);
    private final RateLimiter rateLimiter = mock(RateLimiter.class);
    private final LiveEventPublisher publisher = mock(LiveEventPublisher.class);
    private final PremiumUserService premium = mock(PremiumUserService.class);
    private final ChannelMembershipService members = mock(ChannelMembershipService.class);

    private final UUID owner = UUID.randomUUID();
    private final UUID fan = UUID.randomUUID();
    private LiveStream stream;
    private LiveChatService chat;

    private AppUser user(UUID id, String name, String handle) {
        Channel channel = Channel.builder().id(UUID.randomUUID()).name(name).handle(handle).build();
        AppUser u = AppUser.builder().id(id).email(name + "@x").name(name).picture(name + ".png").channel(channel).build();
        when(userRepo.findById(id)).thenReturn(Optional.of(u));
        return u;
    }

    private LiveChatService newChat() {
        return new LiveChatService(repo, userRepo, redis, rateLimiter, publisher, new ObjectMapper(), premium, members);
    }

    @BeforeEach
    void setUp() {
        stream = LiveStream.builder().id(UUID.randomUUID()).channelId(UUID.randomUUID()).ownerId(owner)
                .title("Buổi phát").source(LiveSource.BROWSER).status(LiveStatus.LIVE).chatEnabled(true)
                .livekitRoom("live-room").createdAt(LocalDateTime.now()).build();
        when(repo.findById(stream.getId())).thenReturn(Optional.of(stream));
        when(rateLimiter.tryAcquire(eq(Limit.LIVE_CHAT), any())).thenReturn(true);
        user(owner, "Chủ kênh", "chukenh");
        user(fan, "Fan", "fan");
        chat = newChat();
    }

    private List<LiveEvent> streamEvents() {
        ArgumentCaptor<LiveEvent> captor = ArgumentCaptor.forClass(LiveEvent.class);
        verify(publisher, org.mockito.Mockito.atLeast(0)).toStream(eq(stream.getId()), captor.capture());
        return captor.getAllValues();
    }

    private String privateErrorTo(UUID userId) {
        ArgumentCaptor<LiveEvent> captor = ArgumentCaptor.forClass(LiveEvent.class);
        verify(publisher).toUser(eq(userId.toString()), captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("ERROR");
        return captor.getValue().getText();
    }

    @Test
    void postBroadcastsTheMessageWithSenderInfoAndOwnerFlag() {
        chat.post(stream.getId(), fan.toString(), "Xin chào");
        chat.post(stream.getId(), owner.toString(), "Chào cả nhà");

        List<LiveEvent> events = streamEvents();
        assertThat(events).extracting(LiveEvent::getType).containsExactly("CHAT", "CHAT");
        LiveChatMessage fromFan = events.get(0).getMessage();
        assertThat(fromFan.text()).isEqualTo("Xin chào");
        assertThat(fromFan.name()).isEqualTo("Fan");
        assertThat(fromFan.avatar()).isEqualTo("Fan.png");
        assertThat(fromFan.handle()).isEqualTo("fan");
        assertThat(fromFan.userId()).isEqualTo(fan.toString());
        assertThat(fromFan.owner()).isFalse();
        assertThat(fromFan.id()).isNotBlank();
        assertThat(events.get(1).getMessage().owner()).isTrue();
        assertThat(chat.recent(stream.getId())).extracting(LiveChatMessage::text).containsExactly("Xin chào", "Chào cả nhà");
    }

    @Test
    void messageFromAPremiumSenderCarriesThePremiumFlag() {
        when(premium.premiumUntil(fan)).thenReturn(Optional.of(java.time.LocalDate.now().plusDays(2)));

        chat.post(stream.getId(), fan.toString(), "Xin chào");
        chat.post(stream.getId(), owner.toString(), "Chào cả nhà");

        List<LiveEvent> events = streamEvents();
        assertThat(events.get(0).getMessage().premium()).isTrue();
        assertThat(events.get(1).getMessage().premium()).isFalse();
    }

    @Test
    void messageOver200CharsIsRejectedWithAPrivateError() {
        chat.post(stream.getId(), fan.toString(), "x".repeat(201));

        assertThat(privateErrorTo(fan)).contains("200");
        verify(publisher, never()).toStream(any(), any());
        assertThat(chat.recent(stream.getId())).isEmpty();
    }

    @Test
    void messageOfExactly200CharsIsAccepted() {
        chat.post(stream.getId(), fan.toString(), "x".repeat(200));

        assertThat(chat.recent(stream.getId())).hasSize(1);
    }

    @Test
    void blankMessageIsIgnoredSilently() {
        chat.post(stream.getId(), fan.toString(), "   ");

        verify(publisher, never()).toStream(any(), any());
        verify(publisher, never()).toUser(anyString(), any());
    }

    @Test
    void rateLimitedUserGetsAPrivateErrorAndNothingIsBroadcast() {
        when(rateLimiter.tryAcquire(Limit.LIVE_CHAT, fan.toString())).thenReturn(false);

        chat.post(stream.getId(), fan.toString(), "spam");

        assertThat(privateErrorTo(fan)).contains("nhanh");
        verify(publisher, never()).toStream(any(), any());
    }

    @Test
    void disabledChatRejectsPosts() {
        stream.setChatEnabled(false);

        chat.post(stream.getId(), fan.toString(), "hello");

        assertThat(privateErrorTo(fan)).contains("tắt");
        assertThat(chat.recent(stream.getId())).isEmpty();
    }

    @Test
    void chatOnlyWorksWhileTheStreamIsLive() {
        stream.setStatus(LiveStatus.CREATED);
        chat.post(stream.getId(), fan.toString(), "sớm quá");
        stream.setStatus(LiveStatus.ENDED);
        chat.post(stream.getId(), fan.toString(), "muộn quá");

        verify(publisher, never()).toStream(any(), any());
    }

    @Test
    void unknownSenderIsIgnored() {
        UUID ghost = UUID.randomUUID();
        when(userRepo.findById(ghost)).thenReturn(Optional.empty());

        chat.post(stream.getId(), ghost.toString(), "boo");

        verify(publisher, never()).toStream(any(), any());
    }

    @Test
    void keepsOnlyThe100MostRecentMessages() {
        for (int i = 1; i <= 105; i++) {
            chat.post(stream.getId(), fan.toString(), "tin " + i);
        }

        List<LiveChatMessage> recent = chat.recent(stream.getId());
        assertThat(recent).hasSize(100);
        assertThat(recent.get(0).text()).isEqualTo("tin 6");
        assertThat(recent.get(99).text()).isEqualTo("tin 105");
    }

    @Test
    void htmlIsStoredVerbatimAndRenderedAsTextByTheClient() {
        String payload = "<img src=x onerror=alert(1)> & \"quote\"";

        chat.post(stream.getId(), fan.toString(), payload);

        assertThat(chat.recent(stream.getId()).get(0).text()).isEqualTo(payload);
    }

    @Test
    void nonOwnerCannotDeleteAMessage() {
        chat.post(stream.getId(), fan.toString(), "xấu");
        String id = chat.recent(stream.getId()).get(0).id();

        chat.delete(stream.getId(), fan.toString(), id);

        assertThat(privateErrorTo(fan)).isNotBlank();
        assertThat(chat.recent(stream.getId())).hasSize(1);
        assertThat(streamEvents()).extracting(LiveEvent::getType).doesNotContain("CHAT_DELETED");
    }

    @Test
    void ownerDeletesAMessageAndEveryoneIsTold() {
        chat.post(stream.getId(), fan.toString(), "xấu");
        String id = chat.recent(stream.getId()).get(0).id();

        chat.delete(stream.getId(), owner.toString(), id);

        assertThat(chat.recent(stream.getId())).isEmpty();
        LiveEvent last = streamEvents().get(streamEvents().size() - 1);
        assertThat(last.getType()).isEqualTo("CHAT_DELETED");
        assertThat(last.getMessageId()).isEqualTo(id);
    }

    @Test
    void redisFailingDoesNotStopChat() {
        doThrow(new IllegalStateException("redis down")).when(redis).set(anyString(), anyString(), any(Duration.class));

        chat.post(stream.getId(), fan.toString(), "vẫn chạy");

        assertThat(chat.recent(stream.getId())).hasSize(1);
        verify(publisher).toStream(eq(stream.getId()), any(LiveEvent.class));
    }

    @Test
    void messagesArePersistedToRedisWithA13HourTtl() {
        chat.post(stream.getId(), fan.toString(), "lưu lại");

        verify(redis, atLeastOnce()).set(eq("live:chat:" + stream.getId()), anyString(), eq(Duration.ofHours(13)));
    }

    @Test
    void restoresRecentMessagesFromRedisAfterARestart() throws Exception {
        List<LiveChatMessage> saved = List.of(
                new LiveChatMessage("m1", fan.toString(), "Fan", "Fan.png", "fan", "tin cũ", false, 1L, false, null));
        when(redis.get("live:chat:" + stream.getId())).thenReturn(new ObjectMapper().writeValueAsString(saved));

        List<LiveChatMessage> recent = newChat().recent(stream.getId());

        assertThat(recent).extracting(LiveChatMessage::text).containsExactly("tin cũ");
    }

    @Test
    void endedOrDisabledStreamsHaveNoMessagesToShow() {
        chat.post(stream.getId(), fan.toString(), "một tin");

        stream.setChatEnabled(false);
        assertThat(chat.recent(stream.getId())).isEmpty();

        stream.setChatEnabled(true);
        stream.setStatus(LiveStatus.ENDED);
        assertThat(chat.recent(stream.getId())).isEmpty();
    }

    @Test
    void endingTheStreamClearsMemoryAndRedis() {
        chat.post(stream.getId(), fan.toString(), "một tin");

        chat.onEnded(stream.getId());

        verify(redis).delete("live:chat:" + stream.getId());
        stream.setStatus(LiveStatus.LIVE);
        assertThat(chat.recent(stream.getId())).isEmpty();
    }
}
