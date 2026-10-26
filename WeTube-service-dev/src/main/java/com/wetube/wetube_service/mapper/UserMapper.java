package com.wetube.wetube_service.mapper;

import com.wetube.wetube_service.dto.UserDto;
import com.wetube.wetube_service.dto.response.UserResponseDto;
import com.wetube.wetube_service.entity.AppUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    AppUser toEntity(UserDto dto);
    UserResponseDto toResponse(AppUser entity);

    AppUser toEntity(UserResponseDto dto);
    @Mapping(target = "handle", source = "channel.handle")
    UserDto toBasicDto(AppUser entity);
}
