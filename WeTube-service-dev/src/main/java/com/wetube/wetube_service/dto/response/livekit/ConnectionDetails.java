package com.wetube.wetube_service.dto.response.livekit;


import lombok.Data;

@Data
public class ConnectionDetails {
    private String token;
    private String wsUrl;
}
