package com.wetube.wetube_service.dto.response.livekit;

import lombok.Data;

@Data
public class TrackDto {
    private String sid;
    private String name;
    private String type;
    private boolean muted;
    private String source;
}
