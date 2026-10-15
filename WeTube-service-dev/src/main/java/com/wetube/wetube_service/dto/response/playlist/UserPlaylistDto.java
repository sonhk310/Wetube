package com.wetube.wetube_service.dto.response.playlist;

import java.time.LocalDateTime;
import java.util.UUID;

import com.wetube.wetube_service.enumeration.PlaylistType;

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
public class UserPlaylistDto {
    private UUID playlistId;
    private String playlistTitle;
    private PlaylistType playlistType;
    private int totalVideos;
    private LocalDateTime createdAt;
    private String privacy;   
    private String lastUpdatedLabel;
    private String thumbnailUrl; 
}
