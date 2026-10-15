package com.wetube.wetube_service.dto.request.livekit;

import lombok.Data;

@Data
public class CreateIngressParams {
    private String roomName;
    private String ingressType;
    private RoomMetadata metadata;
}
