package com.wetube.wetube_service.dto.response.livekit;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ParticipantDto {
    private String sid;
    private String identity;
    private JsonNode metadata;
    private  String name;
    private long joinedAt;
    private long joinedAtMs;
    private String region;
    private boolean publisher;
    private int version;
    private String state;
    private String kind;
    private List<String> kindDetails;
    private JsonNode permission;
    private Map<String, String> attributes;
    private String disconnectReason;
    private List<TrackDto> tracks;
    private boolean isMuted;
}
