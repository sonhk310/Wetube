package com.wetube.wetube_service.mapper;

import com.wetube.wetube_service.dto.UserDto;
import com.wetube.wetube_service.dto.response.UserResponseDto;
import com.wetube.wetube_service.entity.AppUser;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    AppUser toEntity(UserDto dto);
    UserResponseDto toResponse(AppUser entity);

    AppUser toEntity(UserResponseDto dto);
    UserDto toBasicDto(AppUser entity);
}
