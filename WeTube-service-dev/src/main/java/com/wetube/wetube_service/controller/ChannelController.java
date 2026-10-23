package com.wetube.wetube_service.controller;

import com.wetube.wetube_service.dto.response.ChannelResponseDto;
import com.wetube.wetube_service.service.channel.ChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
public class ChannelController {
    private final ChannelService channelService;

    @GetMapping("/{channelId}")
    public ResponseEntity<ChannelResponseDto> getChannelById(@PathVariable UUID channelId, @AuthenticationPrincipal Jwt jwt) {
        UUID viewerId = (jwt != null) ? UUID.fromString(jwt.getSubject()) : null;
        ChannelResponseDto response = channelService.getChannelById(channelId, viewerId);
        return ResponseEntity.ok(response);
    }
}
