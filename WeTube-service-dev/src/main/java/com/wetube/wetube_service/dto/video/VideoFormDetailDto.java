package com.wetube.wetube_service.dto.video;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VideoFormDetailDto {
    private UUID id;
    private String title;
    private String description;
    private String videoUrl;
    private String thumbnailUrl;
    private String status;
    private float duration;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Set<TagDto> tags;
}