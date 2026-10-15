package com.wetube.wetube_service.dto.request.livekit;

import lombok.Data;

@Data
public class CreateStreamParams {
    private String roomName;
    private RoomMetadata metadata;
}
