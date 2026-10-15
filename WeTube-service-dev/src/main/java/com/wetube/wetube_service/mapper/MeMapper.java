package com.wetube.wetube_service.mapper;

import com.wetube.wetube_service.dto.response.MeResponseDto;
import com.wetube.wetube_service.entity.AppUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MeMapper {
    @Mapping(target = "sub", expression = "java(user.getId() != null ? user.getId().toString() : null)")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "picture", source = "picture")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "roles", expression = "java(new java.util.ArrayList<>(user.getRoleCodes()))")
    @Mapping(target = "channelId", expression = "java(user.getChannel() != null && user.getChannel().getId() != null ? user.getChannel().getId().toString() : null)")
    @Mapping(target = "channel", source = "channel")
    @Mapping(target = "playlists", source = "playlists")
    MeResponseDto toDto(AppUser user);
}
