package com.wetube.wetube_service.entity.report;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Báo cáo vi phạm của người xem đối với một video, một bình luận dưới video (commentId khác null)
 * hoặc một kênh (channelId khác null, videoId null);
 * admin xử lý trong trang quản trị của client.
 */
@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    public enum Status { OPEN, RESOLVED, DISMISSED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    /** Null khi báo cáo một kênh (channelId). */
    @Column(name = "video_id", columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID videoId;

    /** Có giá trị khi báo cáo một kênh ("Báo cáo người dùng" ở hộp Giới thiệu của kênh). */
    @Column(name = "channel_id", columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID channelId;

    /** Có giá trị khi báo cáo một bình luận (videoId là video chứa bình luận đó). */
    @Column(name = "comment_id", columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID commentId;

    @Column(name = "reporter_id", nullable = false, columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID reporterId;

    @Column(nullable = false, length = 50)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    /** Ghi chú của admin khi xử lý (vd. "Đã ẩn video"). */
    @Column(name = "resolution_note", length = 255)
    private String resolutionNote;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
