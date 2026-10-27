package com.wetube.wetube_service.controller;

import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wetube.wetube_service.dto.video.RecommendVideoDto;
import com.wetube.wetube_service.security.CurrentUser;
import com.wetube.wetube_service.service.video.HomeFeedService;
import com.wetube.wetube_service.service.video.TopicCatalog;

import lombok.RequiredArgsConstructor;

/** Chip và video theo chip ở trang chủ; cần đăng nhập. */
@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private static final int MAX_PAGE_SIZE = 50;

    private final HomeFeedService homeFeedService;
    private final TopicCatalog topicCatalog;

    /** Các chip đang có video (ẩn chip rỗng); rỗng cả hàng nếu chưa có lịch sử xem. */
    @GetMapping("/chips")
    public ResponseEntity<List<HomeFeedService.Chip>> chips(
            @RequestParam(defaultValue = "") String seed,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(homeFeedService.chips(CurrentUser.id(jwt), seed));
    }

    /** chip = SUBSCRIPTIONS | NEW | UNWATCHED | WATCHED | topic:{id chủ đề} (sai thì 400). */
    @GetMapping("/feed")
    public ResponseEntity<Page<RecommendVideoDto>> feed(
            @RequestParam String chip,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "18") int size,
            @RequestParam(defaultValue = "") String seed,
            @AuthenticationPrincipal Jwt jwt) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        if (chip.startsWith(HomeFeedService.TOPIC_PREFIX)) {
            String topicId = chip.substring(HomeFeedService.TOPIC_PREFIX.length());
            if (!topicCatalog.exists(topicId)) {
                throw new IllegalArgumentException("Chip không hợp lệ: " + chip);
            }
            return ResponseEntity.ok(homeFeedService.topicFeed(CurrentUser.id(jwt), topicId, pageable));
        }
        HomeFeedService.Kind kind;
        try {
            kind = HomeFeedService.Kind.valueOf(chip.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Chip không hợp lệ: " + chip);
        }
        return ResponseEntity.ok(homeFeedService.feed(CurrentUser.id(jwt), kind, pageable, seed));
    }
}
