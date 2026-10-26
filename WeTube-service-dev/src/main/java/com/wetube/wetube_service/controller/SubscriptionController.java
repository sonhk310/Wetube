package com.wetube.wetube_service.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.wetube.wetube_service.security.CurrentUser;
import java.util.List;
import java.util.UUID;

import com.wetube.wetube_service.dto.request.SubscriptionRequest;
import com.wetube.wetube_service.dto.request.UnsubscribeRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import com.wetube.wetube_service.enumeration.SubscriptionType;
import org.springframework.web.bind.annotation.*;

import com.wetube.wetube_service.dto.response.SubscribedChannelResponseDto;
import com.wetube.wetube_service.service.channel.SubscriptionService;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/subscriptions")
@AllArgsConstructor
public class SubscriptionController {
private final  SubscriptionService subscriptionService;
    @GetMapping("/{userId}")
    public ResponseEntity<List<SubscribedChannelResponseDto>> getSubscribedChannels(@PathVariable UUID userId) {
        return ResponseEntity.ok(subscriptionService.getSubscribedChannels(userId));
    }

    @PostMapping("/subscribe")
    public ResponseEntity<String> subscribe(@RequestBody SubscriptionRequest request, @AuthenticationPrincipal Jwt jwt) {
        request.setSubscriberId(CurrentUser.id(jwt));
        subscriptionService.subscribe(request);
        return ResponseEntity.ok("Subscribed successfully");
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<String> unsubscribe(@RequestBody UnsubscribeRequest request, @AuthenticationPrincipal Jwt jwt) {
        request.setSubscriberId(CurrentUser.id(jwt));
        subscriptionService.unsubscribe(request);
        return ResponseEntity.ok("Unsubscribed successfully");
    }

    /** Đổi chế độ thông báo (chuông) của người gọi với một kênh đã đăng ký. */
    @PutMapping("/mode")
    public ResponseEntity<String> updateMode(@RequestBody ModeRequest request, @AuthenticationPrincipal Jwt jwt) {
        if (request.channelId() == null || request.mode() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu channelId hoặc mode");
        }
        subscriptionService.updateSubscriptionMode(CurrentUser.id(jwt), request.channelId(), null, request.mode());
        return ResponseEntity.ok("Updated");
    }

    public record ModeRequest(UUID channelId, SubscriptionType mode) {
    }
}
