package com.wetube.wetube_service.controller;

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
            @RequestParam PlaylistType playlistType) {

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
    public ResponseEntity<UserPlaylistDto> createPlaylist(@RequestBody CreatePlaylistRequest request) {
        return ResponseEntity.ok(playlistService.createPlaylist(request));
    }

    @PostMapping("/add")
    public ResponseEntity<PlaylistVideoDto> addVideoToPlaylist(@RequestBody PlaylistaddRequest request) {
        return ResponseEntity.ok(playlistService.addVideoToPlaylist(request));
    }

    @DeleteMapping("/{playlistId}/{videoId}")
    public ResponseEntity<Map<String, String>> removeVideoFromPlaylist(
            @PathVariable UUID playlistId,
            @PathVariable UUID videoId) {

        playlistService.removeVideoFromPlaylist(playlistId, videoId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Video removed from playlist successfully");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{playlistId}")
    public ResponseEntity<Map<String, String>> removePlaylist(@PathVariable UUID playlistId) {
        playlistService.removePlaylist(playlistId);
        return ResponseEntity.ok(Map.of("message", "Playlist deleted successfully"));
    }

    @GetMapping("/{userId}/history")
    public ResponseEntity<List<PlaylistVideoDetailDto>> getUserHistory(@PathVariable UUID userId) {
        return ResponseEntity.ok(playlistService.getHistoryByUser(userId));
    }

    @PostMapping("/{userId}/history/add/{videoId}")
    public ResponseEntity<List<PlaylistVideoDetailDto>> addToHistory(
            @PathVariable UUID userId,
            @PathVariable UUID videoId) {
        return ResponseEntity.ok(playlistService.addVideoToHistory(userId, videoId));
    }

}
