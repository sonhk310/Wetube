package com.wetube.wetube_service.entity.video;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.playlist.PlaylistVideo;
import com.wetube.wetube_service.enumeration.ActiveStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "videos")
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_id", nullable = false)
    private AppUser user;

    @Column(nullable = false)
    private String title;
    @Column(columnDefinition = "TEXT")
    private String description;
    /** Từ khóa tìm kiếm do chủ video duyệt (phân cách dấu phẩy); chỉ dùng cho tìm kiếm, không phải thẻ. */
    @Column(name = "search_keywords", columnDefinition = "TEXT")
    private String searchKeywords;
    /** Chủ đề (C8): id trong topics.json, nhóm hoặc chủ đề con; null với bản nháp và video cũ chưa chọn. */
    @Column(name = "topic_id", length = 100)
    private String topicId;

    private String thumbnailUrl;
    private String videoUrl;
    @Enumerated(EnumType.STRING)
    private ActiveStatus videosStatus;
    /** Trạng thái trước khi admin ẩn (INACTIVE), để "Bỏ ẩn" trả lại đúng; null nếu không bị admin ẩn. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status_before_hidden", length = 20)
    private ActiveStatus statusBeforeHidden;
    /** true khi video bị ẩn cùng kênh ("Ẩn kênh"): "Bỏ ẩn kênh" chỉ mở lại những video này. */
    @Column(name = "hidden_with_channel")
    private Boolean hiddenWithChannel;
    private int totalView;
    private float duration;

    @Column(nullable = false)
    private boolean isShort;

    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Builder.Default
    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<VideoTag> videoTags = new HashSet<>();

    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlaylistVideo> playlistVideos;

    @Builder.Default
    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<VideoSubtitle> subtitles = new ArrayList<>();


}
