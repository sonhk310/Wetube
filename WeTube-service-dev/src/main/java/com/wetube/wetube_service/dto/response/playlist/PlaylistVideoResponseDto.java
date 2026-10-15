package com.wetube.wetube_service.dto.response.playlist;

import java.time.LocalDateTime;
import java.util.UUID;


import com.wetube.wetube_service.dto.video.VideoDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaylistVideoResponseDto {
    private UUID id;
    private VideoDto video;
    private float historyDuration;
    private LocalDateTime updatedAt;
}