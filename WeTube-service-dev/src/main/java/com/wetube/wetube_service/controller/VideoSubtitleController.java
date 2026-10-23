package com.wetube.wetube_service.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wetube.wetube_service.dto.request.CreateSubtitleRequest;
import com.wetube.wetube_service.dto.video.VideoSubtitleDto;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.security.CurrentUser;
import com.wetube.wetube_service.security.OwnershipGuard;
import com.wetube.wetube_service.security.RateLimiter;
import com.wetube.wetube_service.service.AI.SubtitleAiService;
import com.wetube.wetube_service.service.video.VideoSubtitleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoSubtitleController {

    private final VideoSubtitleService subtitleService;
    private final SubtitleAiService subtitleAiService;
    private final VideoRepository videoRepository;
    private final OwnershipGuard ownershipGuard;
    private final RateLimiter rateLimiter;

    @PostMapping("/{videoId}/subtitles")
    public ResponseEntity<VideoSubtitleDto> addSubtitle(
            @PathVariable UUID videoId,
            @RequestBody CreateSubtitleRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        ownershipGuard.videoOwner(videoId, jwt);
        VideoSubtitleDto dto = subtitleService.addSubtitle(videoId, request);
        return ResponseEntity.ok(dto);
    }

    /** Chủ video yêu cầu tạo phụ đề AI (Gemini) cho video đã có; chạy nền, trả 202 ngay. */
    @PostMapping("/{videoId}/subtitles/ai")
    public ResponseEntity<Map<String, String>> generateAiSubtitle(
            @PathVariable UUID videoId,
            @RequestParam String targetLang,
            @AuthenticationPrincipal Jwt jwt) {
        ownershipGuard.videoOwner(videoId, jwt);
        if (!SubtitleAiService.isSupported(targetLang)) {
            throw new IllegalArgumentException("Ngôn ngữ chưa hỗ trợ: " + targetLang);
        }
        rateLimiter.check(RateLimiter.Limit.AI_SUBTITLE, CurrentUser.id(jwt));
        String videoUrl = videoRepository.findById(videoId).orElseThrow().getVideoUrl();
        subtitleAiService.generateAsync(videoId, videoUrl, targetLang);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("message", "Đang tạo phụ đề, vui lòng tải lại trang sau ít phút."));
    }
}
