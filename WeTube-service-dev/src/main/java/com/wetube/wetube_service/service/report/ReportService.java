package com.wetube.wetube_service.service.report;

import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.channel.Channel;
import com.wetube.wetube_service.entity.interaction.Comment;
import com.wetube.wetube_service.entity.report.Report;
import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.exception.ResourceNotFoundException;
import com.wetube.wetube_service.repository.UserRepository;
import com.wetube.wetube_service.repository.channel.ChannelRepository;
import com.wetube.wetube_service.repository.interaction.CommentRepository;
import com.wetube.wetube_service.repository.report.ReportRepository;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.service.VideoIndexer;
import com.wetube.wetube_service.service.interaction.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {

    public static final Set<String> REASONS = Set.of(
            "SPAM", "VIOLENCE", "SEXUAL", "HATE", "COPYRIGHT", "MISINFORMATION", "OTHER");

    /** Lý do báo cáo người dùng (kênh), theo YouTube. */
    public static final Set<String> CHANNEL_REASONS = Set.of(
            "IMPERSONATION", "INAPPROPRIATE_PROFILE", "SPAM", "HATE", "OTHER");

    /**
     * commentId != null: báo cáo bình luận; commentContent / commentAuthor null nếu bình luận đã bị xoá.
     * channelId != null: báo cáo kênh (các trường video null); channelName null nếu kênh không còn.
     */
    public record ReportView(UUID id, UUID videoId, String videoTitle, String thumbnailUrl,
                             String videoStatus, UUID commentId, String commentContent, String commentAuthor,
                             UUID channelId, String channelName, String channelHandle, String channelPicture,
                             String channelStatus,
                             String reason, String details, String status,
                             String resolutionNote, LocalDateTime createdAt, LocalDateTime resolvedAt,
                             AdminInfo admin) {}

    /**
     * Chỉ có trong danh sách của admin, để xét báo cáo đúng hay sai: người báo cáo và các báo cáo khác của họ
     * (đã xử lý = đúng, không vi phạm = sai), số người đã báo cáo cùng video / bình luận / kênh và số báo cáo
     * theo từng lý do.
     */
    public record AdminInfo(String reporterName, String reporterPicture, long reporterConfirmed,
                            long reporterDismissed, long targetReporters, Map<String, Long> targetReasonCounts) {}

    public enum Action { HIDE_VIDEO, DELETE_COMMENT, HIDE_CHANNEL, UNHIDE_VIDEO, UNHIDE_CHANNEL, DISMISS }

    /** Video của kênh bị ẩn cùng kênh (bản nháp, riêng tư, đã ẩn / xoá thì giữ nguyên). */
    private static final List<ActiveStatus> HIDDEN_WITH_CHANNEL =
            List.of(ActiveStatus.ACTIVE, ActiveStatus.PUBLIC, ActiveStatus.UNLISTED);

    private final ReportRepository reportRepository;
    private final VideoRepository videoRepository;
    private final VideoIndexer videoIndexer;
    private final CommentRepository commentRepository;
    private final CommentService commentService;
    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;

    /**
     * channelId khác null: báo cáo kênh đó; commentId khác null: báo cáo bình luận đó (videoId lấy theo bình
     * luận, tham số videoId bỏ qua); còn lại báo cáo video.
     */
    public ReportView create(UUID reporterId, UUID videoId, UUID commentId, UUID channelId, String reason,
                             String details) {
        if (channelId != null) {
            return createChannelReport(reporterId, channelId, reason, details);
        }
        if (!REASONS.contains(reason)) {
            throw new IllegalArgumentException("Lý do báo cáo không hợp lệ: " + reason);
        }
        if (commentId != null) {
            Comment comment = commentRepository.findById(commentId)
                    .filter(c -> c.getTargetType() == Comment.TargetType.VIDEO)
                    .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId.toString()));
            if (comment.getUser() != null && reporterId.equals(comment.getUser().getId())) {
                throw new IllegalArgumentException("Bạn không thể báo cáo bình luận của chính mình.");
            }
            if (reportRepository.existsByCommentIdAndReporterIdAndStatus(commentId, reporterId, Report.Status.OPEN)) {
                throw new IllegalArgumentException("Bạn đã báo cáo bình luận này, báo cáo đang chờ xử lý.");
            }
            videoId = comment.getTargetId();
        } else {
            if (videoId == null) {
                throw new IllegalArgumentException("Thiếu videoId hoặc commentId");
            }
            if (!videoRepository.existsById(videoId)) {
                throw new ResourceNotFoundException("Video", "id", videoId.toString());
            }
            if (reportRepository.existsByVideoIdAndCommentIdIsNullAndReporterIdAndStatus(
                    videoId, reporterId, Report.Status.OPEN)) {
                throw new IllegalArgumentException("Bạn đã báo cáo video này, báo cáo đang chờ xử lý.");
            }
        }
        Report report = reportRepository.save(Report.builder()
                .videoId(videoId)
                .commentId(commentId)
                .reporterId(reporterId)
                .reason(reason)
                .details(details == null ? null : details.strip())
                .status(Report.Status.OPEN)
                .createdAt(LocalDateTime.now())
                .build());
        return toViews(List.of(report)).get(0);
    }

    private ReportView createChannelReport(UUID reporterId, UUID channelId, String reason, String details) {
        if (!CHANNEL_REASONS.contains(reason)) {
            throw new IllegalArgumentException("Lý do báo cáo không hợp lệ: " + reason);
        }
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", "id", channelId.toString()));
        if (channel.getUser() != null && reporterId.equals(channel.getUser().getId())) {
            throw new IllegalArgumentException("Bạn không thể báo cáo kênh của chính mình.");
        }
        if (reportRepository.existsByChannelIdAndReporterIdAndStatus(channelId, reporterId, Report.Status.OPEN)) {
            throw new IllegalArgumentException("Bạn đã báo cáo kênh này, báo cáo đang chờ xử lý.");
        }
        Report report = reportRepository.save(Report.builder()
                .channelId(channelId)
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
        List<Report> reports = status == null
                ? reportRepository.findAllByOrderByCreatedAtDesc()
                : reportRepository.findByStatusOrderByCreatedAtDesc(status);
        return toViews(reports, adminInfo(reports));
    }

    /** Video / bình luận / kênh mà báo cáo nhắm tới. */
    private static String targetKey(Report r) {
        if (r.getChannelId() != null) return "channel:" + r.getChannelId();
        if (r.getCommentId() != null) return "comment:" + r.getCommentId();
        return "video:" + r.getVideoId();
    }

    private Map<UUID, AdminInfo> adminInfo(List<Report> reports) {
        if (reports.isEmpty()) {
            return Map.of();
        }
        // Mọi báo cáo về cùng các đối tượng này (kể cả báo cáo đã đóng), gom theo đối tượng
        List<Report> sameTargets = new ArrayList<>();
        sameTargets.addAll(reportRepository.findByChannelIdIn(
                reports.stream().map(Report::getChannelId).filter(Objects::nonNull).distinct().toList()));
        sameTargets.addAll(reportRepository.findByCommentIdIn(
                reports.stream().map(Report::getCommentId).filter(Objects::nonNull).distinct().toList()));
        sameTargets.addAll(reportRepository.findByVideoIdInAndCommentIdIsNullAndChannelIdIsNull(reports.stream()
                .filter(r -> r.getChannelId() == null && r.getCommentId() == null)
                .map(Report::getVideoId).filter(Objects::nonNull).distinct().toList()));
        Map<String, List<Report>> byTarget = sameTargets.stream().collect(Collectors.groupingBy(ReportService::targetKey));

        List<UUID> reporterIds = reports.stream().map(Report::getReporterId).distinct().toList();
        Map<UUID, List<Report>> byReporter = reportRepository.findByReporterIdIn(reporterIds).stream()
                .collect(Collectors.groupingBy(Report::getReporterId));
        Map<UUID, AppUser> reporters = userRepository.findAllById(reporterIds).stream()
                .collect(Collectors.toMap(AppUser::getId, Function.identity()));

        Map<UUID, AdminInfo> result = new HashMap<>();
        for (Report r : reports) {
            List<Report> target = byTarget.getOrDefault(targetKey(r), List.of(r));
            // Lịch sử của người báo cáo, không tính chính báo cáo này
            List<Report> history = byReporter.getOrDefault(r.getReporterId(), List.of()).stream()
                    .filter(o -> !o.getId().equals(r.getId())).toList();
            AppUser reporter = reporters.get(r.getReporterId());
            result.put(r.getId(), new AdminInfo(
                    reporter != null ? reporter.getName() : null,
                    reporter != null ? reporter.getPicture() : null,
                    history.stream().filter(o -> o.getStatus() == Report.Status.RESOLVED).count(),
                    history.stream().filter(o -> o.getStatus() == Report.Status.DISMISSED).count(),
                    target.stream().map(Report::getReporterId).distinct().count(),
                    target.stream().collect(Collectors.groupingBy(Report::getReason, TreeMap::new,
                            Collectors.counting()))));
        }
        return result;
    }

    /**
     * Admin xử lý báo cáo. HIDE_VIDEO: ẩn video (INACTIVE, gỡ khỏi tìm kiếm/gợi ý) và đóng mọi báo
     * cáo đang mở của video đó; DELETE_COMMENT: xoá bình luận bị báo cáo (kèm các trả lời) và đóng mọi
     * báo cáo đang mở của bình luận đó; HIDE_CHANNEL: ẩn kênh (INACTIVE: trang kênh báo không tìm thấy, không
     * hiện khi tìm kênh) cùng các video đang xem được của kênh, đóng mọi báo cáo đang mở của kênh đó;
     * UNHIDE_VIDEO / UNHIDE_CHANNEL: bỏ ẩn, video về trạng thái trước khi bị ẩn (bỏ ẩn kênh chỉ mở lại video
     * bị ẩn cùng kênh); DISMISS: bác bỏ riêng báo cáo này.
     */
    public ReportView resolve(UUID reportId, Action action) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", "id", reportId.toString()));
        LocalDateTime now = LocalDateTime.now();

        if (action == Action.DISMISS) {
            report.setStatus(Report.Status.DISMISSED);
            report.setResolutionNote("Không vi phạm");
            report.setResolvedAt(now);
            return toViews(List.of(report)).get(0);
        }

        if (action == Action.DELETE_COMMENT) {
            UUID commentId = report.getCommentId();
            if (commentId == null) {
                throw new IllegalArgumentException("Báo cáo này không phải báo cáo bình luận");
            }
            if (commentRepository.existsById(commentId)) {
                commentService.deleteComment(commentId);
            }
            for (Report open : reportRepository.findByCommentIdAndStatus(commentId, Report.Status.OPEN)) {
                open.setStatus(Report.Status.RESOLVED);
                open.setResolutionNote("Đã xoá bình luận");
                open.setResolvedAt(now);
            }
            report.setStatus(Report.Status.RESOLVED);
            report.setResolutionNote("Đã xoá bình luận");
            report.setResolvedAt(now);
            return toViews(List.of(report)).get(0);
        }

        if (action == Action.UNHIDE_CHANNEL) {
            UUID channelId = report.getChannelId();
            if (channelId == null) {
                throw new IllegalArgumentException("Báo cáo này không phải báo cáo kênh");
            }
            Channel channel = channelRepository.findById(channelId)
                    .orElseThrow(() -> new ResourceNotFoundException("Channel", "id", channelId.toString()));
            if (channel.getStatus() != ActiveStatus.INACTIVE) {
                throw new IllegalArgumentException("Kênh này không bị ẩn");
            }
            channel.setStatus(ActiveStatus.ACTIVE);
            if (channel.getUser() != null) {
                for (Video video : videoRepository.findByUser_IdAndVideosStatusIn(
                        channel.getUser().getId(), List.of(ActiveStatus.INACTIVE))) {
                    if (Boolean.TRUE.equals(video.getHiddenWithChannel())) {
                        restore(video);
                    }
                }
            }
            report.setResolutionNote("Đã bỏ ẩn kênh");
            return toViews(List.of(report)).get(0);
        }

        if (action == Action.UNHIDE_VIDEO) {
            if (report.getVideoId() == null) {
                throw new IllegalArgumentException("Báo cáo này không phải báo cáo video");
            }
            Video video = videoRepository.findById(report.getVideoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Video", "id", report.getVideoId().toString()));
            if (video.getVideosStatus() != ActiveStatus.INACTIVE) {
                throw new IllegalArgumentException("Video này không bị ẩn");
            }
            Channel channel = video.getUser() != null ? video.getUser().getChannel() : null;
            if (channel != null && channel.getStatus() == ActiveStatus.INACTIVE) {
                throw new IllegalArgumentException("Kênh của video này đang bị ẩn, hãy bỏ ẩn kênh trước.");
            }
            restore(video);
            report.setResolutionNote("Đã bỏ ẩn video");
            return toViews(List.of(report)).get(0);
        }

        if (action == Action.HIDE_CHANNEL) {
            UUID channelId = report.getChannelId();
            if (channelId == null) {
                throw new IllegalArgumentException("Báo cáo này không phải báo cáo kênh");
            }
            Channel channel = channelRepository.findById(channelId)
                    .orElseThrow(() -> new ResourceNotFoundException("Channel", "id", channelId.toString()));
            channel.setStatus(ActiveStatus.INACTIVE);
            if (channel.getUser() != null) {
                for (Video video : videoRepository.findByUser_IdAndVideosStatusIn(
                        channel.getUser().getId(), HIDDEN_WITH_CHANNEL)) {
                    video.setStatusBeforeHidden(video.getVideosStatus());
                    video.setHiddenWithChannel(true);
                    video.setVideosStatus(ActiveStatus.INACTIVE);
                    videoIndexer.remove(video.getId());
                }
            }
            for (Report open : reportRepository.findByChannelIdAndStatus(channelId, Report.Status.OPEN)) {
                open.setStatus(Report.Status.RESOLVED);
                open.setResolutionNote("Kênh đã bị ẩn");
                open.setResolvedAt(now);
            }
            report.setStatus(Report.Status.RESOLVED);
            report.setResolutionNote("Kênh đã bị ẩn");
            report.setResolvedAt(now);
            return toViews(List.of(report)).get(0);
        }

        if (report.getVideoId() == null) {
            throw new IllegalArgumentException("Báo cáo này không phải báo cáo video");
        }
        Video video = videoRepository.findById(report.getVideoId())
                .orElseThrow(() -> new ResourceNotFoundException("Video", "id", report.getVideoId().toString()));
        // Đã bị ẩn cùng kênh thì giữ trạng thái gốc; từ nay là ẩn riêng nên "Bỏ ẩn kênh" không mở lại video này
        if (video.getVideosStatus() != ActiveStatus.INACTIVE) {
            video.setStatusBeforeHidden(video.getVideosStatus());
        }
        video.setHiddenWithChannel(false);
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

    /** Trả video bị admin ẩn về trạng thái trước đó (không rõ thì công khai) và đưa lại vào tìm kiếm. */
    private void restore(Video video) {
        video.setVideosStatus(video.getStatusBeforeHidden() != null ? video.getStatusBeforeHidden() : ActiveStatus.PUBLIC);
        video.setStatusBeforeHidden(null);
        video.setHiddenWithChannel(null);
        videoIndexer.index(video);
    }

    private List<ReportView> toViews(List<Report> reports) {
        return toViews(reports, Map.of());
    }

    private List<ReportView> toViews(List<Report> reports, Map<UUID, AdminInfo> adminInfo) {
        Map<UUID, Video> videos = videoRepository
                .findAllById(reports.stream().map(Report::getVideoId).filter(Objects::nonNull).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Video::getId, Function.identity()));
        // ChannelRepository là CrudRepository: findAllById trả Iterable
        Map<UUID, Channel> channels = StreamSupport.stream(channelRepository
                        .findAllById(reports.stream().map(Report::getChannelId).filter(Objects::nonNull).distinct().toList())
                        .spliterator(), false)
                .collect(Collectors.toMap(Channel::getId, Function.identity()));
        Map<UUID, Comment> comments = commentRepository
                .findAllById(reports.stream().map(Report::getCommentId).filter(Objects::nonNull).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Comment::getId, Function.identity()));
        return reports.stream().map(r -> {
            Video v = videos.get(r.getVideoId());
            Comment c = r.getCommentId() != null ? comments.get(r.getCommentId()) : null;
            Channel ch = r.getChannelId() != null ? channels.get(r.getChannelId()) : null;
            boolean isChannelReport = r.getChannelId() != null;
            return new ReportView(r.getId(), r.getVideoId(),
                    v != null ? v.getTitle() : isChannelReport ? null : "(video đã bị xoá)",
                    v != null ? v.getThumbnailUrl() : null,
                    v != null && v.getVideosStatus() != null ? v.getVideosStatus().name() : null,
                    r.getCommentId(),
                    c != null ? c.getContent() : null,
                    c != null && c.getUser() != null ? c.getUser().getName() : null,
                    r.getChannelId(),
                    ch != null ? ch.getName() : null,
                    ch != null ? ch.getHandle() : null,
                    ch != null ? ch.getPicture() : null,
                    ch != null && ch.getStatus() != null ? ch.getStatus().name() : null,
                    r.getReason(), r.getDetails(), r.getStatus().name(), r.getResolutionNote(),
                    r.getCreatedAt(), r.getResolvedAt(), adminInfo.isEmpty() ? null : adminInfo.get(r.getId()));
        }).toList();
    }
}
