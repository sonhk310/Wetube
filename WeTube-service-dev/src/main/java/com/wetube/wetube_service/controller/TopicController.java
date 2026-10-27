package com.wetube.wetube_service.controller;

import java.time.Duration;
import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wetube.wetube_service.service.video.TopicCatalog;

import lombok.RequiredArgsConstructor;

/** Danh sách chủ đề cố định (công khai); chỉ đổi khi deploy. */
@RestController
@RequestMapping("/api/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicCatalog topicCatalog;

    @GetMapping
    public ResponseEntity<List<TopicCatalog.Group>> topics() {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(topicCatalog.groups());
    }
}
