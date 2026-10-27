package com.wetube.wetube_service.repository.report;

import com.wetube.wetube_service.entity.report.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    List<Report> findByReporterIdOrderByCreatedAtDesc(UUID reporterId);

    List<Report> findByStatusOrderByCreatedAtDesc(Report.Status status);

    List<Report> findAllByOrderByCreatedAtDesc();

    boolean existsByVideoIdAndCommentIdIsNullAndReporterIdAndStatus(UUID videoId, UUID reporterId, Report.Status status);

    boolean existsByCommentIdAndReporterIdAndStatus(UUID commentId, UUID reporterId, Report.Status status);

    List<Report> findByCommentIdAndStatus(UUID commentId, Report.Status status);

    List<Report> findByVideoIdAndStatus(UUID videoId, Report.Status status);

    boolean existsByChannelIdAndReporterIdAndStatus(UUID channelId, UUID reporterId, Report.Status status);

    List<Report> findByChannelIdAndStatus(UUID channelId, Report.Status status);
}
