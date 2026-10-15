package com.wetube.wetube_service.mapper;

import com.wetube.wetube_service.dto.response.livekit.ParticipantDto;
import com.wetube.wetube_service.utility.ParticipantMapperHelper;
import livekit.LivekitModels;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = ParticipantMapperHelper.class)
public interface ParticipantMapper {

    @Mapping(target = "sid", source = "sid")
    @Mapping(target = "name", source = "identity")
    @Mapping(target = "metadata", source = "metadata", qualifiedByName = "stringToJson")
    @Mapping(target = "joinedAt", source = "joinedAt")
    @Mapping(target = "joinedAtMs", source = "joinedAtMs")
    @Mapping(target = "region", source = "region")
    @Mapping(target = "publisher", source = "isPublisher")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "state", expression = "java(p.getState().name())")
    @Mapping(target = "kind", expression = "java(p.getKind().name())")
    @Mapping(target = "kindDetails", source = "kindDetailsList", qualifiedByName = "kindDetailsToList")
    @Mapping(target = "permission", source = "permission", qualifiedByName = "permissionToJson")
    @Mapping(target = "attributes", expression = "java(p.getAttributesMap())")
    @Mapping(target = "disconnectReason", expression = "java(p.getDisconnectReason().name())")
    @Mapping(target = "tracks", source = "tracksList", qualifiedByName = "mapTracks")
    @Mapping(target = "muted", expression = "java(p.getTracksList().stream().allMatch(livekit.LivekitModels.TrackInfo::getMuted))")
    ParticipantDto toDto(LivekitModels.ParticipantInfo p);

    List<ParticipantDto> toDtoList(List<LivekitModels.ParticipantInfo> list);
}
