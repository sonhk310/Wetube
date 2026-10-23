package com.wetube.wetube_service.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.wetube.wetube_service.security.CurrentUser;
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

    @GetMapping
    public ResponseEntity<LikeDto> getLikeInfo(
            @RequestParam UUID targetId,
            @RequestParam Like.TargetType targetType,
            @RequestParam(required = false) UUID userId
    ) {
        return ResponseEntity.ok(likeService.getLikeInfo(targetId, targetType, userId));
    }

    @PostMapping("/toggle")
    public ResponseEntity<Void> toggleLike(
            @RequestParam UUID targetId,
            @RequestParam Like.TargetType targetType,
            @AuthenticationPrincipal Jwt jwt
    ) {
        likeService.toggleLike(targetId, targetType, CurrentUser.id(jwt));
        return ResponseEntity.ok().build();
    }
}
