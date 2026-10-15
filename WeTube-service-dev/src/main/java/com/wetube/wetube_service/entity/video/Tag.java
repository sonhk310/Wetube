package com.wetube.wetube_service.entity.video;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)

@Table(name = "tags", indexes = @Index(name = "uk_tag_name", columnList = "name", unique = true))
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String name;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Builder.Default
    private Integer count = 0;


    @PrePersist
    void prePersist() {
        if (createdAt == null)
            createdAt = LocalDateTime.now();

        if (count == null)
            count = 0;
    }

    @Builder.Default
    @OneToMany(mappedBy = "tag", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<VideoTag> videoTags = new HashSet<>();
}
