package com.wetube.wetube_service.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.wetube.wetube_service.security.CurrentUser;
import com.wetube.wetube_service.security.OwnershipGuard;
import com.wetube.wetube_service.dto.channel.ChannelStructureResponse;
import com.wetube.wetube_service.dto.channel.UpdateCategoryOrderRequest;
import com.wetube.wetube_service.dto.channel.UpdateVideoOrderRequest;
import com.wetube.wetube_service.service.channel.ChannelManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/channel")
@Slf4j
@Validated
public class ChannelManagementController {

    private final ChannelManagementService channelManagementService;
    private final OwnershipGuard ownershipGuard;

    // Lấy cấu trúc channel (categories + videos)
    @GetMapping("/{channelId}/structure")
    public ResponseEntity<ChannelStructureResponse> getChannelStructure(
            @PathVariable UUID channelId,
            @RequestParam(defaultValue = "true") boolean includeVideos,
            Authentication authentication) {

        ChannelStructureResponse response = channelManagementService
                .getChannelStructure(channelId, includeVideos);
        return ResponseEntity.ok(response);
    }

    // Cập nhật thứ tự categories
    @PutMapping("/{channelId}/categories/order")
    public ResponseEntity<Map<String, Object>> updateCategoryOrder(
            @PathVariable UUID channelId,
            @RequestBody @Valid UpdateCategoryOrderRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        ownershipGuard.channelOwner(channelId, jwt);

        channelManagementService.updateCategoryOrder(channelId, request.getCategories());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Category order updated successfully"
        ));
    }

    // Cập nhật thứ tự videos trong category
    @PutMapping("/{channelId}/videos/order")
    public ResponseEntity<Map<String, Object>> updateVideoOrder(
            @PathVariable UUID channelId,
            @RequestBody @Valid UpdateVideoOrderRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        ownershipGuard.channelOwner(channelId, jwt);

        channelManagementService.updateVideoOrderInCategory(
                channelId,
                request.getCategoryId(),
                request.getVideos()
        );
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Video order updated successfully"
        ));
    }

    @PostMapping("/{channelId}/categories/{categoryId}/videos")
    public ResponseEntity<Map<String, Object>> addVideoToCategory(
            @PathVariable UUID channelId,
            @PathVariable UUID categoryId,
            @RequestBody Map<String, UUID> requestBody,
            @AuthenticationPrincipal Jwt jwt) {
        ownershipGuard.channelOwner(channelId, jwt);

        UUID videoId = requestBody.get("videoId");
        if (videoId == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "videoId is required"));
        }

        channelManagementService.addVideoToCategory(channelId, categoryId, videoId);

        return ResponseEntity.ok(Map.of("success", true, "message", "Video added to category successfully"));
    }

    @DeleteMapping("/{channelId}/categories/{categoryId}/videos/{videoId}")
    public ResponseEntity<Map<String, Object>> removeVideoFromCategory(
            @PathVariable UUID channelId,
            @PathVariable UUID categoryId,
            @PathVariable UUID videoId,
            @AuthenticationPrincipal Jwt jwt) {
        ownershipGuard.channelOwner(channelId, jwt);

        channelManagementService.removeVideoFromCategory(channelId, categoryId, videoId);

        return ResponseEntity.ok(Map.of("success", true, "message", "Video removed from category successfully"));
    }
}