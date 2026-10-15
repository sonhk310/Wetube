package com.wetube.wetube_service.dto.room;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Room {
    private String roomId;
    private List<WatchMember> members = new ArrayList<>();
    private List<VideoRoom> playlist = new ArrayList<>();
    private MediaPlayerState playerState = new MediaPlayerState();
}
