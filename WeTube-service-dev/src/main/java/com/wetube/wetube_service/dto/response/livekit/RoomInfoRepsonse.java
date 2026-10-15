package com.wetube.wetube_service.dto.response.livekit;

import com.wetube.wetube_service.dto.request.livekit.RoomMetadata;
import lombok.Data;

@Data
public class RoomInfoRepsonse {
    private String roomId;
    private String name;
    private int numParticipants;
    private RoomMetadata metadata;

}
