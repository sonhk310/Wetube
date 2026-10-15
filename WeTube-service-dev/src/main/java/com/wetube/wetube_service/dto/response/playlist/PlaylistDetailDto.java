package com.wetube.wetube_service.dto.response.playlist;

import com.wetube.wetube_service.enumeration.PlaylistType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PlaylistDetailDto {
    private UUID playlistId;
    private String playlistTitle;
    private PlaylistType playlistType;
    private int totalVideos;
    private LocalDateTime createdAt;
    private List<PlaylistVideoDetailDto> videos;
}