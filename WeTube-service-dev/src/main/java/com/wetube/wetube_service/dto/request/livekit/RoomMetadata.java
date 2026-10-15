package com.wetube.wetube_service.dto.request.livekit;

import lombok.Data;

@Data
public class RoomMetadata {
    private String creatorIdentity;
    private boolean enableChat;
    private boolean allowParticipation;

}
