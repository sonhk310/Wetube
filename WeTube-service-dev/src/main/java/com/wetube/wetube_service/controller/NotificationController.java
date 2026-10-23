package com.wetube.wetube_service.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wetube.wetube_service.security.CurrentUser;
import com.wetube.wetube_service.service.notification.NotificationService;
import com.wetube.wetube_service.service.notification.NotificationService.NotificationPage;

import lombok.RequiredArgsConstructor;

/** Thông báo của chính người dùng đang đăng nhập. */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private static final int MAX_PAGE_SIZE = 50;

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<NotificationPage> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return ResponseEntity.ok(notificationService.list(CurrentUser.id(jwt), Math.max(page, 0), safeSize));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(Map.of("count", notificationService.unreadCount(CurrentUser.id(jwt))));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        notificationService.markRead(CurrentUser.id(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal Jwt jwt) {
        notificationService.markAllRead(CurrentUser.id(jwt));
        return ResponseEntity.noContent().build();
    }
}
