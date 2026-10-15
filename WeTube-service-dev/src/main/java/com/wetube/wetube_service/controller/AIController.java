package com.wetube.wetube_service.controller;

import com.wetube.wetube_service.dto.ai.DescriptionRequest;
import com.wetube.wetube_service.dto.ai.TranscriptRequest;
import com.wetube.wetube_service.service.AI.AIService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final AIService aiService;

    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/description")
    public ResponseEntity<?> generateDescription(@RequestBody DescriptionRequest request) {
        String result = aiService.generateVideoDescription(request.getTitle(), request.getTags());
        return ResponseEntity.ok(Map.of("description", result));
    }

    @PostMapping("/transcript")
    public ResponseEntity<?> generateTranscript(@RequestBody TranscriptRequest request) {
        String transcript = aiService.generateTranscript(request.getVideoId(), request.getLang());
        return ResponseEntity.ok(Map.of("transcript", transcript));
    }
}
