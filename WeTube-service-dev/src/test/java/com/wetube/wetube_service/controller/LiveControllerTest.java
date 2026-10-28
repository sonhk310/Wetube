package com.wetube.wetube_service.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import com.wetube.wetube_service.dto.live.CreateLiveRequest;
import com.wetube.wetube_service.dto.live.LiveChatMessage;
import com.wetube.wetube_service.dto.live.LiveRecordingInfo;
import com.wetube.wetube_service.dto.live.LiveTokenResponse;
import com.wetube.wetube_service.dto.live.LiveView;
import com.wetube.wetube_service.dto.live.OpenPollRequest;
import com.wetube.wetube_service.dto.live.VoteRequest;
import com.wetube.wetube_service.enumeration.LiveSource;
import com.wetube.wetube_service.security.RateLimiter;
import com.wetube.wetube_service.security.RateLimiter.Limit;
import com.wetube.wetube_service.service.live.LiveChatService;
import com.wetube.wetube_service.service.live.LivePollService;
import com.wetube.wetube_service.service.live.LiveStreamService;

/** Controller chỉ chuyển danh tính từ JWT (không tin nội dung request) và áp giới hạn tần suất. */
class LiveControllerTest {

    private final LiveStreamService service = mock(LiveStreamService.class);
    private final RateLimiter rateLimiter = mock(RateLimiter.class);
    private final LiveChatService chat = mock(LiveChatService.class);
    private final LivePollService polls = mock(LivePollService.class);
    private final LiveController controller = new LiveController(service, rateLimiter, chat, polls);

    private static Jwt jwtOf(UUID userId, String... roles) {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(userId.toString());
        when(jwt.getClaimAsStringList("roles")).thenReturn(List.of(roles));
        return jwt;
    }

    @Test
    void createUsesTheLoggedInUserNotAnythingFromTheBody() {
        UUID me = UUID.randomUUID();
        CreateLiveRequest body = new CreateLiveRequest("Tiêu đề", null, LiveSource.BROWSER, null, true, null, null);
        LiveView created = LiveView.builder().id(UUID.randomUUID()).build();
        when(service.create(me, body)).thenReturn(created);

        assertThat(controller.create(body, jwtOf(me))).isSameAs(created);
    }

    @Test
    void recordingQuotaIsForTheLoggedInUserAndIs401Otherwise() {
        UUID me = UUID.randomUUID();
        LiveRecordingInfo info = new LiveRecordingInfo(true, 600, null);
        when(service.recordingInfo(me)).thenReturn(info);

        assertThat(controller.recording(jwtOf(me))).isSameAs(info);
        assertThatThrownBy(() -> controller.recording(null))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void createWithoutLoginIs401() {
        assertThatThrownBy(() -> controller.create(new CreateLiveRequest("a", null, null, null, null, null, null), null))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verify(service, never()).create(any(), any());
    }

    @Test
    void mineIs204WhenThereIsNoUnfinishedStream() {
        UUID me = UUID.randomUUID();
        when(service.mine(me)).thenReturn(Optional.empty());

        assertThat(controller.mine(jwtOf(me)).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void anonymousVisitorCanReadAStreamAndTheLiveList() {
        UUID id = UUID.randomUUID();
        LiveView view = LiveView.builder().id(id).build();
        when(service.get(id, null)).thenReturn(view);

        assertThat(controller.get(id, null)).isSameAs(view);
        controller.list();
        verify(service).listLive();
    }

    @Test
    void endPassesTheAdminFlagFromTheJwt() {
        UUID id = UUID.randomUUID();
        UUID admin = UUID.randomUUID();
        UUID owner = UUID.randomUUID();

        controller.end(id, jwtOf(admin, "ROLE_ADMIN"));
        controller.end(id, jwtOf(owner, "ROLE_USER"));

        verify(service).end(admin, true, id);
        verify(service).end(owner, false, id);
    }

    @Test
    void tokenIsRateLimitedPerClientIpAndFirstForwardedAddressWins() {
        UUID id = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.7, 10.0.0.1");
        LiveTokenResponse token = new LiveTokenResponse("t", "wss://x", "VIEWER", false);
        when(service.token(id, null, false)).thenReturn(token);

        assertThat(controller.token(id, false, request, null)).isSameAs(token);

        verify(rateLimiter).check(Limit.LIVE_TOKEN, "203.0.113.7");
    }

    @Test
    void thePublishFlagAndTheLoggedInUserGoToTheService() {
        UUID id = UUID.randomUUID();
        UUID me = UUID.randomUUID();
        LiveTokenResponse token = new LiveTokenResponse("t", "wss://x", "PUBLISHER", false);
        when(service.token(id, me, true)).thenReturn(token);

        assertThat(controller.token(id, true, new MockHttpServletRequest(), jwtOf(me))).isSameAs(token);
    }

    @Test
    void tooManyTokenRequestsStopBeforeTheService() {
        UUID id = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Quá nhiều yêu cầu"))
                .when(rateLimiter).check(eq(Limit.LIVE_TOKEN), any());

        assertThatThrownBy(() -> controller.token(id, false, new MockHttpServletRequest(), null))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
        verify(service, never()).token(any(), any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void recentChatIsReadableWithoutLogin() {
        UUID id = UUID.randomUUID();
        List<LiveChatMessage> messages = List.of(new LiveChatMessage("m1", "u", "Fan", null, null, "xin chào", false, 1L));
        when(chat.recent(id)).thenReturn(messages);

        assertThat(controller.chat(id)).isEqualTo(messages);
    }

    @Test
    void pollEndpointsUseTheLoggedInUserNotTheBody() {
        UUID me = UUID.randomUUID();
        UUID stream = UUID.randomUUID();
        UUID poll = UUID.randomUUID();
        OpenPollRequest open = new OpenPollRequest("?", List.of("a", "b"));

        controller.openPoll(stream, open, jwtOf(me));
        controller.closePoll(stream, poll, jwtOf(me));
        controller.vote(stream, poll, new VoteRequest(1), jwtOf(me));

        verify(polls).open(me, stream, open);
        verify(polls).close(me, stream, poll);
        verify(polls).vote(me, stream, poll, 1);
    }

    @Test
    void voteWithoutAnOptionIs400() {
        assertThatThrownBy(() -> controller.vote(UUID.randomUUID(), UUID.randomUUID(), new VoteRequest(null),
                jwtOf(UUID.randomUUID())))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(polls, never()).vote(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void channelLivesUseTheRequestedPage() {
        UUID channel = UUID.randomUUID();

        controller.byChannel(channel, 2, 12);

        verify(service).byChannel(channel, PageRequest.of(2, 12));
    }
}
