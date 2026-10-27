package com.wetube.wetube_service.entity.video;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Điểm yêu thích chủ đề của người dùng (C8), giống {@link UserTag}; nguồn của chip chủ đề ở trang chủ. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_topic")
@IdClass(UserTopic.UserTopicId.class)
public class UserTopic {

    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID userId;

    @Id
    @Column(length = 100)
    private String topicId;

    @Column(nullable = false)
    private double point;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserTopicId implements Serializable {
        private UUID userId;
        private String topicId;
    }
}
