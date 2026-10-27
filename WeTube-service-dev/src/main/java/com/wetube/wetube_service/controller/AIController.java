package com.wetube.wetube_service.controller;

import com.wetube.wetube_service.dto.ai.DescriptionRequest;
import com.wetube.wetube_service.dto.ai.KeywordsRequest;
import com.wetube.wetube_service.dto.ai.TranscriptRequest;
import com.wetube.wetube_service.security.CurrentUser;
import com.wetube.wetube_service.security.RateLimiter;
import com.wetube.wetube_service.service.AI.AIService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final AIService aiService;
    private final RateLimiter rateLimiter;

    public AIController(AIService aiService, RateLimiter rateLimiter) {
        this.aiService = aiService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/description")
    public ResponseEntity<?> generateDescription(@RequestBody DescriptionRequest request, @AuthenticationPrincipal Jwt jwt) {
        rateLimiter.check(RateLimiter.Limit.AI_TEXT, CurrentUser.id(jwt));
        String result = aiService.generateVideoDescription(request.getTitle(), request.getTags());
        return ResponseEntity.ok(Map.of("description", result));
    }

    @PostMapping("/keywords")
    public ResponseEntity<?> suggestKeywords(@RequestBody KeywordsRequest request, @AuthenticationPrincipal Jwt jwt) {
        rateLimiter.check(RateLimiter.Limit.AI_TEXT, CurrentUser.id(jwt));
        return ResponseEntity.ok(Map.of("keywords",
                aiService.suggestSearchKeywords(request.getTitle(), request.getDescription(), request.getTags())));
    }

    @PostMapping("/transcript")
    public ResponseEntity<?> generateTranscript(@RequestBody TranscriptRequest request, @AuthenticationPrincipal Jwt jwt) {
        rateLimiter.check(RateLimiter.Limit.AI_TEXT, CurrentUser.id(jwt));
        String transcript = aiService.generateTranscript(request.getVideoId(), request.getLang());
        return ResponseEntity.ok(Map.of("transcript", transcript));
    }
}
