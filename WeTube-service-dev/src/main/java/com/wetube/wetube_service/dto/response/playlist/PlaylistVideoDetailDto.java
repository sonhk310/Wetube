package com.wetube.wetube_service.dto.response.playlist;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class PlaylistVideoDetailDto {
    private UUID videoId;
    private String videoTitle;
    private String videoUrl;
    private String thumbnailUrl;
    private String description;
    private float duration;
    private int totalView;
    private LocalDate createdAt;
    private LocalDate updatedAt;
}
