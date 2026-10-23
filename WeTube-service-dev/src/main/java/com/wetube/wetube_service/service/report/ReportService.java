package com.wetube.wetube_service.service.report;

import com.wetube.wetube_service.entity.report.Report;
import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.exception.ResourceNotFoundException;
import com.wetube.wetube_service.repository.report.ReportRepository;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.service.VideoIndexer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {

    public static final Set<String> REASONS = Set.of(
            "SPAM", "VIOLENCE", "SEXUAL", "HATE", "COPYRIGHT", "MISINFORMATION", "OTHER");

    public record ReportView(UUID id, UUID videoId, String videoTitle, String thumbnailUrl,
                             String videoStatus, String reason, String details, String status,
                             String resolutionNote, LocalDateTime createdAt, LocalDateTime resolvedAt) {}

    private final ReportRepository reportRepository;
    private final VideoRepository videoRepository;
    private final VideoIndexer videoIndexer;

    public ReportView create(UUID reporterId, UUID videoId, String reason, String details) {
        if (!REASONS.contains(reason)) {
            throw new IllegalArgumentException("Lý do báo cáo không hợp lệ: " + reason);
        }
        if (!videoRepository.existsById(videoId)) {
            throw new ResourceNotFoundException("Video", "id", videoId.toString());
        }
        if (reportRepository.existsByVideoIdAndReporterIdAndStatus(videoId, reporterId, Report.Status.OPEN)) {
            throw new IllegalArgumentException("Bạn đã báo cáo video này, báo cáo đang chờ xử lý.");
        }
        Report report = reportRepository.save(Report.builder()
                .videoId(videoId)
                .reporterId(reporterId)
                .reason(reason)
                .details(details == null ? null : details.strip())
                .status(Report.Status.OPEN)
                .createdAt(LocalDateTime.now())
                .build());
        return toViews(List.of(report)).get(0);
    }

    @Transactional(readOnly = true)
    public List<ReportView> mine(UUID reporterId) {
        return toViews(reportRepository.findByReporterIdOrderByCreatedAtDesc(reporterId));
    }

    @Transactional(readOnly = true)
    public List<ReportView> list(Report.Status status) {
        return toViews(status == null
                ? reportRepository.findAllByOrderByCreatedAtDesc()
                : reportRepository.findByStatusOrderByCreatedAtDesc(status));
    }

    /**
     * Admin xử lý báo cáo. hideVideo=true: ẩn video (INACTIVE, gỡ khỏi tìm kiếm/gợi ý) và đóng
     * mọi báo cáo đang mở của video đó; false: bác bỏ riêng báo cáo này.
     */
    public ReportView resolve(UUID reportId, boolean hideVideo) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", "id", reportId.toString()));
        LocalDateTime now = LocalDateTime.now();

        if (!hideVideo) {
            report.setStatus(Report.Status.DISMISSED);
            report.setResolutionNote("Không vi phạm");
            report.setResolvedAt(now);
            return toViews(List.of(report)).get(0);
        }

        Video video = videoRepository.findById(report.getVideoId())
                .orElseThrow(() -> new ResourceNotFoundException("Video", "id", report.getVideoId().toString()));
        video.setVideosStatus(ActiveStatus.INACTIVE);
        videoIndexer.remove(video.getId());
        for (Report open : reportRepository.findByVideoIdAndStatus(video.getId(), Report.Status.OPEN)) {
            open.setStatus(Report.Status.RESOLVED);
            open.setResolutionNote("Video đã bị ẩn");
            open.setResolvedAt(now);
        }
        report.setStatus(Report.Status.RESOLVED);
        report.setResolutionNote("Video đã bị ẩn");
        report.setResolvedAt(now);
        return toViews(List.of(report)).get(0);
    }

    private List<ReportView> toViews(List<Report> reports) {
        Map<UUID, Video> videos = videoRepository
                .findAllById(reports.stream().map(Report::getVideoId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Video::getId, Function.identity()));
        return reports.stream().map(r -> {
            Video v = videos.get(r.getVideoId());
            return new ReportView(r.getId(), r.getVideoId(),
                    v != null ? v.getTitle() : "(video đã bị xoá)",
                    v != null ? v.getThumbnailUrl() : null,
                    v != null && v.getVideosStatus() != null ? v.getVideosStatus().name() : null,
                    r.getReason(), r.getDetails(), r.getStatus().name(), r.getResolutionNote(),
                    r.getCreatedAt(), r.getResolvedAt());
        }).toList();
    }
}
