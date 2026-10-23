package com.wetube.wetube_service.controller;

import com.wetube.wetube_service.service.video.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final VideoService videoService;

    /** Các tag dùng nhiều nhất trong video đang công khai, dùng làm chip lọc ở trang chủ. */
    @GetMapping("/popular")
    public ResponseEntity<List<Map<String, Object>>> popular(@RequestParam(defaultValue = "12") int limit) {
        return ResponseEntity.ok(videoService.getPopularTags(Math.min(limit, 30)));
    }
}
