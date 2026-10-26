package com.wetube.wetube_service.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.wetube.wetube_service.security.CurrentUser;
import com.wetube.wetube_service.service.watchRoom.WatchRoomService;

import lombok.RequiredArgsConstructor;

/** Phòng xem chung: tạo phòng và kiểm tra phòng còn không. Phần thời gian thực nằm ở {@link RoomSocketController}. */
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    public record CreateRoomRequest(String videoId, Double t) {
    }

    private final WatchRoomService rooms;

    @PostMapping
    public ResponseEntity<Map<String, String>> create(
            @RequestBody(required = false) CreateRoomRequest body,
            @AuthenticationPrincipal Jwt jwt) {
        String code = rooms.create(
                CurrentUser.id(jwt),
                body != null ? body.videoId() : null,
                body != null ? body.t() : null);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("code", code));
    }

    @GetMapping("/{code}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable String code, @AuthenticationPrincipal Jwt jwt) {
        CurrentUser.id(jwt);
        Map<String, Object> summary = rooms.summary(code);
        if (summary == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Phòng không tồn tại hoặc đã đóng");
        }
        return ResponseEntity.ok(summary);
    }
}
