package com.wetube.wetube_service.dto.request;

import java.util.UUID;

import com.wetube.wetube_service.enumeration.PlaylistType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreatePlaylistRequest {
    private String title;
    private UUID userId;
    private PlaylistType type;
    private String privacy;
}
