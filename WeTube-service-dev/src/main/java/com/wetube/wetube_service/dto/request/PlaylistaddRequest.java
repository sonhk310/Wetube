package com.wetube.wetube_service.dto.request;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PlaylistaddRequest {
   private UUID playlistId;
   private UUID videoId;
   private float historyDuration;
}
