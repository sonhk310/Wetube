package com.wetube.wetube_service.entity.video;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.wetube.wetube_service.enumeration.SubtitleStatus;

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
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "video_subtitles")
@Data
public class VideoSubtitle {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    private String language;
    /** File .vtt trên Cloudinary; chỉ có khi đã công khai. */
    @Column(name = "subtitle_url")
    private String subtitleUrl;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    private SubtitleStatus status;

    /** Bản để sửa: JSON [{start, end, text, original}] (giây). Null với phụ đề cũ trước E2 (chỉ có file .vtt). */
    @Column(columnDefinition = "MEDIUMTEXT")
    private String cues;

    @Column(name = "ai_generated")
    private Boolean aiGenerated;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Người xem chỉ thấy phụ đề đã công khai; dòng cũ (status null) đã công khai từ trước. */
    public boolean isPublished() {
        return (status == null || status == SubtitleStatus.PUBLISHED) && subtitleUrl != null;
    }
}
