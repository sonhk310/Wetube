package com.wetube.wetube_service.entity.notification;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Thông báo cho một người dùng (chuông trên thanh trên cùng). */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    public enum Type {
        /** Kênh đã đăng ký có video mới. */
        NEW_VIDEO,
        /** Có người trả lời bình luận của mình. */
        COMMENT_REPLY,
        /** Có người bình luận video của mình. */
        VIDEO_COMMENT,
        /** Chủ video thả tim bình luận của mình. */
        COMMENT_HEART,
        /** Có người nhắc @handle của mình trong bình luận. */
        COMMENT_MENTION,
        /** Kênh đã đăng ký bắt đầu phát trực tiếp. */
        LIVE_STARTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Column(name = "recipient_id", nullable = false, columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Type type;

    /** Người gây ra thông báo (chủ kênh / người bình luận). */
    @Column(name = "actor_id", columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID actorId;

    @Column(name = "video_id", columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID videoId;

    @Column(name = "comment_id", columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID commentId;

    /** Buổi phát trực tiếp (thông báo LIVE_STARTED). */
    @Column(name = "live_id", columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID liveId;

    /** Trích đoạn bình luận, hoặc tiêu đề buổi phát. */
    @Column(length = 255)
    private String text;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
