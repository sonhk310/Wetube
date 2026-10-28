package com.wetube.wetube_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.wetube.wetube_service.dto.live.ChannelLivesResponse;
import com.wetube.wetube_service.dto.live.CreateLiveRequest;
import com.wetube.wetube_service.dto.live.LiveCard;
import com.wetube.wetube_service.dto.live.LiveChatMessage;
import com.wetube.wetube_service.dto.live.LiveTokenResponse;
import com.wetube.wetube_service.dto.live.LiveView;
import com.wetube.wetube_service.dto.live.OpenPollRequest;
import com.wetube.wetube_service.dto.live.PollView;
import com.wetube.wetube_service.dto.live.UpdateLiveRequest;
import com.wetube.wetube_service.dto.live.VoteRequest;
import com.wetube.wetube_service.security.CurrentUser;
import com.wetube.wetube_service.security.RateLimiter;
import com.wetube.wetube_service.security.RateLimiter.Limit;
import com.wetube.wetube_service.service.live.LiveChatService;
import com.wetube.wetube_service.service.live.LivePollService;
import com.wetube.wetube_service.service.live.LiveStreamService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Phát trực tiếp. Danh tính luôn lấy từ JWT đăng nhập, không bao giờ từ nội dung request. Đọc buổi phát, danh sách và
 * lấy token xem là công khai (khách xem được); mọi thao tác còn lại cần đăng nhập (xem SecurityConfig).
 */
@RestController
@RequestMapping("/api/live")
@RequiredArgsConstructor
public class LiveController {

    private static final int MAX_PAGE_SIZE = 50;

    private final LiveStreamService service;
    private final RateLimiter rateLimiter;
    private final LiveChatService chat;
    private final LivePollService polls;

    @PostMapping
    public LiveView create(@RequestBody CreateLiveRequest body, @AuthenticationPrincipal Jwt jwt) {
        return service.create(CurrentUser.id(jwt), body);
    }

    /** Buổi chưa kết thúc của kênh người gọi (kèm Ingress), 204 nếu không có. */
    @GetMapping("/me")
    public ResponseEntity<LiveView> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.mine(CurrentUser.id(jwt)).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PatchMapping("/{id}")
    public LiveView update(@PathVariable UUID id, @RequestBody UpdateLiveRequest body, @AuthenticationPrincipal Jwt jwt) {
        return service.update(CurrentUser.id(jwt), id, body);
    }

    @PostMapping("/{id}/thumbnail")
    public LiveView thumbnail(@PathVariable UUID id, @RequestParam("file") MultipartFile file,
                              @AuthenticationPrincipal Jwt jwt) {
        return service.uploadThumbnail(CurrentUser.id(jwt), id, file);
    }

    @PostMapping("/{id}/started")
    public ResponseEntity<Void> started(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        service.markStarted(CurrentUser.id(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/end")
    public ResponseEntity<Void> end(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        service.end(CurrentUser.id(jwt), CurrentUser.isAdmin(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public LiveView get(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return service.get(id, jwt == null ? null : CurrentUser.id(jwt));
    }

    @PostMapping("/{id}/token")
    public LiveTokenResponse token(@PathVariable UUID id, @RequestParam(defaultValue = "false") boolean publish,
                                   HttpServletRequest request, @AuthenticationPrincipal Jwt jwt) {
        rateLimiter.check(Limit.LIVE_TOKEN, clientIp(request));
        return service.token(id, jwt == null ? null : CurrentUser.id(jwt), publish);
    }

    /** Các tin chat gần nhất cho người mới vào (công khai, khách cũng đọc được). */
    @GetMapping("/{id}/chat")
    public List<LiveChatMessage> chat(@PathVariable UUID id) {
        return chat.recent(id);
    }

    @PostMapping("/{id}/polls")
    public PollView openPoll(@PathVariable UUID id, @RequestBody OpenPollRequest body, @AuthenticationPrincipal Jwt jwt) {
        return polls.open(CurrentUser.id(jwt), id, body);
    }

    @PostMapping("/{id}/polls/{pollId}/close")
    public PollView closePoll(@PathVariable UUID id, @PathVariable UUID pollId, @AuthenticationPrincipal Jwt jwt) {
        return polls.close(CurrentUser.id(jwt), id, pollId);
    }

    @PostMapping("/{id}/polls/{pollId}/vote")
    public PollView vote(@PathVariable UUID id, @PathVariable UUID pollId, @RequestBody VoteRequest body,
                         @AuthenticationPrincipal Jwt jwt) {
        if (body == null || body.option() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hãy chọn một lựa chọn");
        }
        return polls.vote(CurrentUser.id(jwt), id, pollId, body.option());
    }

    @GetMapping
    public List<LiveCard> list() {
        return service.listLive();
    }

    @GetMapping("/channel/{channelId}")
    public ChannelLivesResponse byChannel(@PathVariable UUID channelId, @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "12") int size) {
        return service.byChannel(channelId, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE)));
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
