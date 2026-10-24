package com.wetube.wetube_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wetube.wetube_service.security.CurrentUser;
import com.wetube.wetube_service.service.SearchHistoryService;

import lombok.RequiredArgsConstructor;

/** Lịch sử tìm kiếm của người đang đăng nhập (hiện trong ô gợi ý tìm kiếm). */
@RestController
@RequestMapping("/api/search-history")
@RequiredArgsConstructor
public class SearchHistoryController {

    public record RecordRequest(String query) {}

    private static final int MAX_LIMIT = 20;

    private final SearchHistoryService searchHistoryService;

    @GetMapping
    public ResponseEntity<List<String>> list(
            @RequestParam(defaultValue = "") String prefix,
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = CurrentUser.id(jwt);
        return ResponseEntity.ok(searchHistoryService.list(userId, prefix, Math.min(Math.max(limit, 1), MAX_LIMIT)));
    }

    @PostMapping
    public ResponseEntity<Void> record(@RequestBody RecordRequest request, @AuthenticationPrincipal Jwt jwt) {
        searchHistoryService.record(CurrentUser.id(jwt), request.query());
        return ResponseEntity.noContent().build();
    }

    /** Xoá một từ khoá (query) hoặc toàn bộ lịch sử (all=true). */
    @DeleteMapping
    public ResponseEntity<Void> remove(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "false") boolean all,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = CurrentUser.id(jwt);
        if (all) {
            searchHistoryService.clear(userId);
        } else if (query != null) {
            searchHistoryService.remove(userId, query);
        } else {
            throw new IllegalArgumentException("Thiếu query hoặc all=true");
        }
        return ResponseEntity.noContent().build();
    }
}
