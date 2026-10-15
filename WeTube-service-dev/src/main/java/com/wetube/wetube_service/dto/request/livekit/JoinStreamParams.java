package com.wetube.wetube_service.dto.request.livekit;

import lombok.Data;

@Data
public class JoinStreamParams {
    private String roomName;
    private String identity;
}
