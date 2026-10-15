package com.wetube.wetube_service.entity.video;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "translation_segments")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class TranslationSegment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    private int segmentIndex;
    private String start;
    private String end;

    @Column(columnDefinition = "TEXT")
    private String original;
    @Column(columnDefinition = "TEXT")
    private String translated;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_translation_id", nullable = false)
    private VideoTranslation translation;

}
