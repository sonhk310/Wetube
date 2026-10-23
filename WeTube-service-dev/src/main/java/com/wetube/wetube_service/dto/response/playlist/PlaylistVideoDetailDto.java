package com.wetube.wetube_service.dto.response.playlist;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
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
    /** Chế độ hiển thị của video (ACTIVE, UNLISTED, PRIVATE, INACTIVE...), để Studio hiện nhãn. */
    private String videoStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
