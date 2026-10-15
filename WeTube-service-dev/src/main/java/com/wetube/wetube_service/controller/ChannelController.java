package com.wetube.wetube_service.controller;

import com.wetube.wetube_service.dto.response.ChannelResponseDto;
import com.wetube.wetube_service.service.channel.ChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
public class ChannelController {
    private final ChannelService channelService;

    @GetMapping("/{channelId}")
    public ResponseEntity<ChannelResponseDto> getChannelById(@PathVariable UUID channelId) {
        ChannelResponseDto response = channelService.getChannelById(channelId);
        return ResponseEntity.ok(response);
    }
}
