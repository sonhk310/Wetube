package com.wetube.wetube_service.dto.response.playlist;

import java.util.List;
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
public class PlaylistResponseDto {
    private UUID id;
    private String title;
    private PlaylistType playlistType;
    private List<PlaylistVideoResponseDto> videos;
}
