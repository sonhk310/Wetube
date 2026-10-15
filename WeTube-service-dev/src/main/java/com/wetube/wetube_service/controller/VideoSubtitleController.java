package com.wetube.wetube_service.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wetube.wetube_service.dto.request.CreateSubtitleRequest;
import com.wetube.wetube_service.dto.video.VideoSubtitleDto;
import com.wetube.wetube_service.service.video.VideoSubtitleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoSubtitleController {

    private final VideoSubtitleService subtitleService;

    @PostMapping("/{videoId}/subtitles")
    public ResponseEntity<VideoSubtitleDto> addSubtitle(
            @PathVariable UUID videoId,
            @RequestBody CreateSubtitleRequest request) {
        VideoSubtitleDto dto = subtitleService.addSubtitle(videoId, request);
        return ResponseEntity.ok(dto);
    }
}

