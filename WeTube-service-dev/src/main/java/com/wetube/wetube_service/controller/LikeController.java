package com.wetube.wetube_service.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.wetube.wetube_service.security.CurrentUser;
import com.wetube.wetube_service.security.VideoAccess;
import com.wetube.wetube_service.dto.LikeDto;
import com.wetube.wetube_service.entity.interaction.Like;
import com.wetube.wetube_service.service.interaction.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;
    private final VideoAccess videoAccess;

    @GetMapping
    public ResponseEntity<LikeDto> getLikeInfo(
            @RequestParam UUID targetId,
            @RequestParam Like.TargetType targetType,
            @RequestParam(required = false) UUID userId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        // Trạng thái đã thích / không thích luôn theo người đang đăng nhập, không tin userId do client gửi
        UUID viewerId = (jwt != null) ? CurrentUser.id(jwt) : null;
        return ResponseEntity.ok(likeService.getLikeInfo(targetId, targetType, viewerId));
    }

    @PostMapping("/toggle")
    public ResponseEntity<Void> toggleLike(
            @RequestParam UUID targetId,
            @RequestParam Like.TargetType targetType,
            @AuthenticationPrincipal Jwt jwt
    ) {
        if (targetType == Like.TargetType.VIDEO) {
            videoAccess.requireViewable(targetId, jwt);
        }
        likeService.toggleLike(targetId, targetType, CurrentUser.id(jwt));
        return ResponseEntity.ok().build();
    }
}
