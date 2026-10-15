package com.wetube.wetube_service.dto.video;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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
    private LocalDate createdAt;
    private LocalDate updatedAt;
    private Set<TagDto> tags;
}