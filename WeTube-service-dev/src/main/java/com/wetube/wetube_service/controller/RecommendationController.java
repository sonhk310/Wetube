package com.wetube.wetube_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.wetube.wetube_service.dto.video.RecommendVideoDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wetube.wetube_service.service.video.RecService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendationController {
    private final RecService recommendationService;

    @GetMapping("/{userId}")
    public ResponseEntity<Page<RecommendVideoDto>> recommendVideos(
            @PathVariable UUID userId,
            Pageable pageable) {
        return ResponseEntity.ok(recommendationService.recommendVideos(userId, pageable));
    }

    @GetMapping("/scout/{userId}")
    public ResponseEntity<List<RecommendVideoDto>> getTopRankedVideos(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "100") int poolSize,
            @RequestParam(defaultValue = "5") int topN) {
        return ResponseEntity.ok(recommendationService.findTopRankedVideos(userId, poolSize, topN));
    }
}
