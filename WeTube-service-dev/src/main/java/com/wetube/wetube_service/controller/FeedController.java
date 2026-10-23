package com.wetube.wetube_service.controller;

import java.util.List;
import java.util.UUID;

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
import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.mapper.video.VideoMapper;
import com.wetube.wetube_service.repository.channel.SubscriptionRepository;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.security.CurrentUser;
import com.wetube.wetube_service.security.VideoAccess;

import lombok.RequiredArgsConstructor;

/** Bảng tin Đăng ký: video mới của các kênh mình đã đăng ký. */
@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    public record FeedPage(List<RecommendVideoDto> content, boolean last) {
    }

    private static final int MAX_PAGE_SIZE = 50;

    private final SubscriptionRepository subscriptionRepository;
    private final VideoRepository videoRepository;
    private final VideoMapper videoMapper;

    @GetMapping("/subscriptions")
    public ResponseEntity<FeedPage> subscriptions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = CurrentUser.id(jwt);
        List<UUID> channelIds = subscriptionRepository.findSubscribedChannelIds(userId);
        if (channelIds.isEmpty()) {
            return ResponseEntity.ok(new FeedPage(List.of(), true));
        }
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<Video> videos = videoRepository.findFeed(
                channelIds, VideoAccess.LISTED, PageRequest.of(Math.max(page, 0), safeSize));
        return ResponseEntity.ok(new FeedPage(videos.map(videoMapper::toRecommendDto).getContent(), videos.isLast()));
    }
}
