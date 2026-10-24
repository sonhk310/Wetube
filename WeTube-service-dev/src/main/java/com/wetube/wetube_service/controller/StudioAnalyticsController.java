package com.wetube.wetube_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wetube.wetube_service.dto.response.analytics.ChannelAnalyticsDto;
import com.wetube.wetube_service.security.CurrentUser;
import com.wetube.wetube_service.service.analytics.StudioAnalyticsService;

import lombok.RequiredArgsConstructor;

/** Số liệu phân tích của kênh người đang đăng nhập (Studio). */
@RestController
@RequestMapping("/api/studio/analytics")
@RequiredArgsConstructor
public class StudioAnalyticsController {

    private final StudioAnalyticsService analyticsService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChannelAnalyticsDto> get(
            @RequestParam(defaultValue = "28") int days,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(analyticsService.get(CurrentUser.id(jwt), days));
    }
}
