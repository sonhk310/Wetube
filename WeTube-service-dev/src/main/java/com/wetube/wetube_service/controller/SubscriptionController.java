package com.wetube.wetube_service.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.wetube.wetube_service.security.CurrentUser;
import java.util.List;
import java.util.UUID;

import com.wetube.wetube_service.dto.request.SubscriptionRequest;
import com.wetube.wetube_service.dto.request.UnsubscribeRequest;
import org.springframework.http.ResponseEntity;
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
}
