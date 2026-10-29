package com.wetube.wetube_service.dto.video;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.wetube.wetube_service.enumeration.SubtitleStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Phụ đề như chủ video thấy trong Studio / hộp thoại tải lên; cues chỉ có khi lấy một phụ đề để sửa. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleManageDto {
    private UUID id;
    private String language;
    private SubtitleStatus status;
    private boolean aiGenerated;
    private String errorMessage;
    private String subtitleUrl;
    private int cueCount;
    private LocalDateTime updatedAt;
    private List<SubtitleCue> cues;
}
