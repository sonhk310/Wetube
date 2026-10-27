package com.wetube.wetube_service.entity.live;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.wetube.wetube_service.enumeration.LiveProtocol;
import com.wetube.wetube_service.enumeration.LiveSource;
import com.wetube.wetube_service.enumeration.LiveStatus;

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

/**
 * Một buổi phát trực tiếp của một kênh. Cột enum ép VARCHAR (không dùng ENUM của MySQL) để thêm giá trị sau này
 * không cần đổi kiểu cột.
 */
@Entity
@Table(name = "live_streams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveStream {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Column(name = "channel_id", nullable = false, columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID channelId;

    @Column(name = "owner_id", nullable = false, columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID ownerId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private LiveSource source;

    /** Null khi phát bằng webcam. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    private LiveProtocol protocol;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private LiveStatus status;

    @Column(name = "chat_enabled", nullable = false)
    private boolean chatEnabled;

    @Column(name = "livekit_room", length = 100)
    private String livekitRoom;

    @Column(name = "ingress_id", length = 100)
    private String ingressId;

    @Column(name = "ingress_url", length = 500)
    private String ingressUrl;

    @Column(name = "ingress_key", length = 200)
    private String ingressKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    /** Lần cuối LiveKit thấy người phát có hình; dùng để tự kết thúc khi mất tín hiệu. */
    @Column(name = "last_publisher_at")
    private LocalDateTime lastPublisherAt;

    @Column(name = "current_viewers", nullable = false)
    private int currentViewers;

    @Column(name = "peak_viewers", nullable = false)
    private int peakViewers;
}
