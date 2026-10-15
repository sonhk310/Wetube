package com.wetube.wetube_service.dto.response.livekit;
import lombok.Data;

@Data
public class CreateStreamResponse {
    private String authToken;
    private ConnectionDetails connectionDetails;
}