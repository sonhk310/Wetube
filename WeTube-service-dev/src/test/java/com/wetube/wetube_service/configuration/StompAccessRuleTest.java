package com.wetube.wetube_service.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.stomp.StompCommand;

/** Khách chưa đăng nhập chỉ được kết nối và nghe các topic /topic/live/...; mọi thứ khác phải đăng nhập. */
class StompAccessRuleTest {

    @Test
    void guestCanConnect() {
        assertThat(StompAccessRule.allowed(StompCommand.CONNECT, null, false)).isTrue();
    }

    @Test
    void guestCanSubscribeToALiveTopic() {
        assertThat(StompAccessRule.allowed(StompCommand.SUBSCRIBE, "/topic/live/6f1c0a52-aaaa-bbbb-cccc-0123456789ab", false)).isTrue();
    }

    @Test
    void guestCannotSubscribeToWatchRoomTopics() {
        assertThat(StompAccessRule.allowed(StompCommand.SUBSCRIBE, "/topic/rooms/abc-defg-hij", false)).isFalse();
    }

    @Test
    void guestCannotSubscribeToThePrivateQueues() {
        assertThat(StompAccessRule.allowed(StompCommand.SUBSCRIBE, "/user/queue/live", false)).isFalse();
        assertThat(StompAccessRule.allowed(StompCommand.SUBSCRIBE, "/user/queue/rooms", false)).isFalse();
    }

    @Test
    void liveTopicPrefixIsNotFooledByLookalikes() {
        assertThat(StompAccessRule.allowed(StompCommand.SUBSCRIBE, "/topic/liveX/1", false)).isFalse();
        assertThat(StompAccessRule.allowed(StompCommand.SUBSCRIBE, "/topic/live", false)).isFalse();
        assertThat(StompAccessRule.allowed(StompCommand.SUBSCRIBE, null, false)).isFalse();
    }

    @Test
    void guestCannotSendAnything() {
        assertThat(StompAccessRule.allowed(StompCommand.SEND, "/app/live/1/chat", false)).isFalse();
        assertThat(StompAccessRule.allowed(StompCommand.SEND, "/app/rooms/abc/control", false)).isFalse();
    }

    @Test
    void loggedInUserCanDoEverything() {
        assertThat(StompAccessRule.allowed(StompCommand.CONNECT, null, true)).isTrue();
        assertThat(StompAccessRule.allowed(StompCommand.SUBSCRIBE, "/topic/rooms/abc", true)).isTrue();
        assertThat(StompAccessRule.allowed(StompCommand.SUBSCRIBE, "/user/queue/live", true)).isTrue();
        assertThat(StompAccessRule.allowed(StompCommand.SEND, "/app/live/1/chat", true)).isTrue();
    }

    @Test
    void housekeepingCommandsAreAlwaysAllowed() {
        assertThat(StompAccessRule.allowed(StompCommand.DISCONNECT, null, false)).isTrue();
        assertThat(StompAccessRule.allowed(StompCommand.UNSUBSCRIBE, null, false)).isTrue();
    }

    @Test
    void unknownCommandIsAllowedBecauseTheBrokerRejectsWhatItDoesNotKnow() {
        assertThat(StompAccessRule.allowed(null, null, false)).isTrue();
    }
}
