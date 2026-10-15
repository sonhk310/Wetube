package com.wetube.wetube_service.utility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.util.JsonFormat;
import com.wetube.wetube_service.dto.response.livekit.TrackDto;
import livekit.LivekitModels;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ParticipantMapperHelper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Named("stringToJson")
    public JsonNode stringToJson(String value) {
        try {
            return (value != null && !value.isEmpty()) ? objectMapper.readTree(value) : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Named("permissionToJson")
    public JsonNode permissionToJson(LivekitModels.ParticipantPermission perm) {
        try {
            if (perm != null) {
                String json = JsonFormat.printer().print(perm);
                return objectMapper.readTree(json);
            }
        } catch (Exception ignored) {}
        return null;
    }

    @Named("kindDetailsToList")
    public List<String> kindDetailsToList(List<LivekitModels.ParticipantInfo.KindDetail> details) {
        if (details == null) return List.of();
        return details.stream()
                .map(Enum::name)
                .toList();
    }

    @Named("mapTracks")
    public List<TrackDto> mapTracks(List<LivekitModels.TrackInfo> tracks) {
        return tracks.stream().map(t -> {
            TrackDto dto = new TrackDto();
            dto.setSid(t.getSid());
            dto.setName(t.getName());
            dto.setType(t.getType().name());
            dto.setMuted(t.getMuted());
            dto.setSource(t.getSource().name());
            return dto;
        }).toList();
    }
}

