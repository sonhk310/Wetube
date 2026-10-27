package com.wetube.wetube_service.controller;

import com.wetube.wetube_service.entity.report.Report;
import com.wetube.wetube_service.security.CurrentUser;
import com.wetube.wetube_service.security.RateLimiter;
import com.wetube.wetube_service.service.report.ReportService;
import com.wetube.wetube_service.service.report.ReportService.ReportView;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    /** Báo cáo video: videoId; báo cáo bình luận: commentId; báo cáo kênh (người dùng): channelId. */
    public record CreateReportRequest(UUID videoId, UUID commentId, UUID channelId, String reason, String details) {}

    private final ReportService reportService;
    private final RateLimiter rateLimiter;

    /** Người xem báo cáo một video, một bình luận hoặc một kênh. */
    @PostMapping
    public ResponseEntity<ReportView> create(@RequestBody CreateReportRequest request, @AuthenticationPrincipal Jwt jwt) {
        if ((request.videoId() == null && request.commentId() == null && request.channelId() == null)
                || request.reason() == null) {
            throw new IllegalArgumentException("Thiếu videoId/commentId/channelId hoặc lý do báo cáo");
        }
        UUID userId = CurrentUser.id(jwt);
        rateLimiter.check(RateLimiter.Limit.REPORT, userId);
        ReportView view = reportService.create(userId, request.videoId(), request.commentId(), request.channelId(),
                request.reason(), request.details());
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    /** Nhật ký báo cáo của chính người dùng (trang "Report diary"). */
    @GetMapping("/mine")
    public ResponseEntity<List<ReportView>> mine(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(reportService.mine(CurrentUser.id(jwt)));
    }

    /** Admin: danh sách báo cáo, lọc theo trạng thái (OPEN, RESOLVED, DISMISSED). */
    @GetMapping
    public ResponseEntity<List<ReportView>> list(
            @RequestParam(required = false) Report.Status status,
            @AuthenticationPrincipal Jwt jwt) {
        CurrentUser.requireAdmin(jwt);
        return ResponseEntity.ok(reportService.list(status));
    }

    /**
     * Admin: action=HIDE_VIDEO ẩn video, DELETE_COMMENT xoá bình luận bị báo cáo, HIDE_CHANNEL ẩn kênh,
     * UNHIDE_VIDEO / UNHIDE_CHANNEL bỏ ẩn, DISMISS bác bỏ báo cáo.
     */
    @PostMapping("/{reportId}/resolve")
    public ResponseEntity<ReportView> resolve(
            @PathVariable UUID reportId,
            @RequestParam String action,
            @AuthenticationPrincipal Jwt jwt) {
        CurrentUser.requireAdmin(jwt);
        ReportService.Action parsed;
        try {
            parsed = ReportService.Action.valueOf(action);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "action phải là HIDE_VIDEO, DELETE_COMMENT, HIDE_CHANNEL, UNHIDE_VIDEO, UNHIDE_CHANNEL hoặc DISMISS");
        }
        return ResponseEntity.ok(reportService.resolve(reportId, parsed));
    }
}
