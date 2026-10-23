package com.wetube.wetube_service.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.wetube.wetube_service.security.CurrentUser;
import com.wetube.wetube_service.security.OwnershipGuard;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wetube.wetube_service.dto.request.CreatePlaylistRequest;
import com.wetube.wetube_service.dto.request.PlaylistaddRequest;
import com.wetube.wetube_service.dto.response.playlist.PlaylistDetailDto;
import com.wetube.wetube_service.dto.response.playlist.PlaylistVideoDetailDto;
import com.wetube.wetube_service.dto.response.playlist.PlaylistVideoDto;
import com.wetube.wetube_service.dto.response.playlist.UserPlaylistDto;
import com.wetube.wetube_service.enumeration.PlaylistType;
import com.wetube.wetube_service.service.PlaylistService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class PlaylistController {
    private final PlaylistService playlistService;
    private final OwnershipGuard ownershipGuard;

    // @PostMapping("/init/{userId}")
    // public ResponseEntity<Void> createPlaylist(@PathVariable UUID userId) {
    // pls.initiatePlaylist(userId);
    // return ResponseEntity.noContent().build();
    // }

    @Deprecated
    @GetMapping("/all/{userId}")
    public ResponseEntity<List<UserPlaylistDto>> getAllPlaylistsByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(playlistService.getAllPlaylistByUserId(userId));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<UserPlaylistDto>> getUserPlaylistsById(@PathVariable UUID userId) {
        return ResponseEntity.ok(playlistService.getUserPlaylistById(userId));
    }

    @GetMapping("/created/{userId}")
    public ResponseEntity<List<UserPlaylistDto>> getUserCreatedPlaylistsById(@PathVariable UUID userId) {
        return ResponseEntity.ok(playlistService.getUserCreatedPlaylistById(userId));
    }

    @GetMapping("/detail/{playlistId}")
    public ResponseEntity<PlaylistDetailDto> getPlaylistVideoById(@PathVariable UUID playlistId) {
        return ResponseEntity.ok(playlistService.getPlaylistDetailedById(playlistId));
    }

    @GetMapping("/{userId}/playlistType")
    public ResponseEntity<List<UserPlaylistDto>> getAllPlaylistsByTagUser(
            @PathVariable UUID userId,
            @RequestParam PlaylistType playlistType,
            @AuthenticationPrincipal Jwt jwt) {
        if (playlistType == PlaylistType.HISTORY || playlistType == PlaylistType.LIKED) {
            requireSelf(userId, jwt);
        }

        return ResponseEntity.ok(playlistService.getAllPlaylistByTypeUserId(userId, playlistType));
    }

    @GetMapping("/detail")
    public ResponseEntity<PlaylistDetailDto> getPlaylistDetail(
            @RequestParam UUID channelId,
            @RequestParam String playlistName) {

        PlaylistDetailDto result = playlistService.getPlaylistDetailedById(channelId, playlistName);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/create")
    public ResponseEntity<UserPlaylistDto> createPlaylist(@RequestBody CreatePlaylistRequest request, @AuthenticationPrincipal Jwt jwt) {
        request.setUserId(CurrentUser.id(jwt));
        return ResponseEntity.ok(playlistService.createPlaylist(request));
    }

    @PostMapping("/add")
    public ResponseEntity<PlaylistVideoDto> addVideoToPlaylist(@RequestBody PlaylistaddRequest request, @AuthenticationPrincipal Jwt jwt) {
        ownershipGuard.playlistOwner(request.getPlaylistId(), jwt);
        return ResponseEntity.ok(playlistService.addVideoToPlaylist(request));
    }

    @DeleteMapping("/{playlistId}/{videoId}")
    public ResponseEntity<Map<String, String>> removeVideoFromPlaylist(
            @PathVariable UUID playlistId,
            @PathVariable UUID videoId,
            @AuthenticationPrincipal Jwt jwt) {
        ownershipGuard.playlistOwner(playlistId, jwt);

        playlistService.removeVideoFromPlaylist(playlistId, videoId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Video removed from playlist successfully");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{playlistId}")
    public ResponseEntity<Map<String, String>> removePlaylist(@PathVariable UUID playlistId, @AuthenticationPrincipal Jwt jwt) {
        ownershipGuard.playlistOwner(playlistId, jwt);
        playlistService.removePlaylist(playlistId);
        return ResponseEntity.ok(Map.of("message", "Playlist deleted successfully"));
    }

    @GetMapping("/{userId}/history")
    public ResponseEntity<List<PlaylistVideoDetailDto>> getUserHistory(@PathVariable UUID userId, @AuthenticationPrincipal Jwt jwt) {
        requireSelf(userId, jwt);
        return ResponseEntity.ok(playlistService.getHistoryByUser(userId));
    }

    @PostMapping("/{userId}/history/add/{videoId}")
    public ResponseEntity<List<PlaylistVideoDetailDto>> addToHistory(
            @PathVariable UUID userId,
            @PathVariable UUID videoId,
            @AuthenticationPrincipal Jwt jwt) {
        requireSelf(userId, jwt);
        return ResponseEntity.ok(playlistService.addVideoToHistory(userId, videoId));
    }

    /** Thêm / bỏ video khỏi playlist hệ thống của chính user (chỉ LIKED và WATCH_LATER). */
    @PostMapping("/{userId}/system/{type}/{videoId}")
    public ResponseEntity<Void> addToSystemPlaylist(
            @PathVariable UUID userId,
            @PathVariable PlaylistType type,
            @PathVariable UUID videoId,
            @AuthenticationPrincipal Jwt jwt) {
        requireSelf(userId, jwt);
        requireUserEditableType(type);
        playlistService.addVideoToSystemPlaylist(userId, videoId, type);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}/system/{type}/{videoId}")
    public ResponseEntity<Void> removeFromSystemPlaylist(
            @PathVariable UUID userId,
            @PathVariable PlaylistType type,
            @PathVariable UUID videoId,
            @AuthenticationPrincipal Jwt jwt) {
        requireSelf(userId, jwt);
        requireUserEditableType(type);
        playlistService.removeVideoFromSystemPlaylist(userId, videoId, type);
        return ResponseEntity.ok().build();
    }

    private static void requireUserEditableType(PlaylistType type) {
        if (type != PlaylistType.LIKED && type != PlaylistType.WATCH_LATER) {
            throw new IllegalArgumentException("Chỉ hỗ trợ LIKED và WATCH_LATER");
        }
    }

    /** Dữ liệu riêng tư (lịch sử, đã thích): chỉ chủ tài khoản được xem hoặc ghi. */
    private static void requireSelf(UUID userId, Jwt jwt) {
        if (!CurrentUser.id(jwt).equals(userId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Bạn không có quyền với dữ liệu này");
        }
    }
}
