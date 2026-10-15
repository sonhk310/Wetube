package com.wetube.wetube_service.dto.request.livekit;

import lombok.Data;

@Data
public class StopStreamParams {
    private String roomName;
    private String identity;
}
