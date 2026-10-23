package com.wetube.wetube_service.controller;

import com.wetube.wetube_service.entity.report.Report;
import com.wetube.wetube_service.security.CurrentUser;
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

    public record CreateReportRequest(UUID videoId, String reason, String details) {}

    private final ReportService reportService;

    /** Người xem báo cáo một video. */
    @PostMapping
    public ResponseEntity<ReportView> create(@RequestBody CreateReportRequest request, @AuthenticationPrincipal Jwt jwt) {
        if (request.videoId() == null || request.reason() == null) {
            throw new IllegalArgumentException("Thiếu videoId hoặc lý do báo cáo");
        }
        ReportView view = reportService.create(CurrentUser.id(jwt), request.videoId(), request.reason(), request.details());
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

    /** Admin: action=HIDE_VIDEO ẩn video, action=DISMISS bác bỏ báo cáo. */
    @PostMapping("/{reportId}/resolve")
    public ResponseEntity<ReportView> resolve(
            @PathVariable UUID reportId,
            @RequestParam String action,
            @AuthenticationPrincipal Jwt jwt) {
        CurrentUser.requireAdmin(jwt);
        boolean hide = switch (action) {
            case "HIDE_VIDEO" -> true;
            case "DISMISS" -> false;
            default -> throw new IllegalArgumentException("action phải là HIDE_VIDEO hoặc DISMISS");
        };
        return ResponseEntity.ok(reportService.resolve(reportId, hide));
    }
}
