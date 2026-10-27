package com.wetube.wetube_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
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

import com.wetube.wetube_service.dto.live.ChannelLivesResponse;
import com.wetube.wetube_service.dto.live.CreateLiveRequest;
import com.wetube.wetube_service.dto.live.LiveCard;
import com.wetube.wetube_service.dto.live.LiveTokenResponse;
import com.wetube.wetube_service.dto.live.LiveView;
import com.wetube.wetube_service.dto.live.UpdateLiveRequest;
import com.wetube.wetube_service.security.CurrentUser;
import com.wetube.wetube_service.security.RateLimiter;
import com.wetube.wetube_service.security.RateLimiter.Limit;
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
    public LiveTokenResponse token(@PathVariable UUID id, HttpServletRequest request, @AuthenticationPrincipal Jwt jwt) {
        rateLimiter.check(Limit.LIVE_TOKEN, clientIp(request));
        return service.token(id, jwt == null ? null : CurrentUser.id(jwt));
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
