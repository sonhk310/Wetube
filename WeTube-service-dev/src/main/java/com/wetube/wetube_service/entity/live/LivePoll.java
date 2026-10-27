package com.wetube.wetube_service.entity.live;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Khảo sát do chủ kênh mở trong buổi phát; mỗi lúc chỉ một Poll mở. */
@Entity
@Table(name = "live_polls")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LivePoll {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Column(name = "stream_id", nullable = false, columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID streamId;

    @Column(nullable = false, length = 200)
    private String question;

    /** Mảng chuỗi dạng JSON, vd. ["Có","Không"]. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String options;

    @Column(name = "is_open", nullable = false)
    private boolean open;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;
}
