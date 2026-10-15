package com.wetube.wetube_service.dto.room;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaPlayerState {
    private String roomId;
    private boolean playing;
    private long currentTimeMillis;
    private String currentSongId;
}
