package com.wetube.wetube_service.mapper.premium;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.wetube.wetube_service.dto.PremiumUserDto;
import com.wetube.wetube_service.entity.premium.PremiumUser;

@Mapper(componentModel = "spring")
public interface PremiumUserMapper {
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.name")
    @Mapping(target = "subPackId", source = "subpack.id")
    @Mapping(target = "subPackName", source = "subpack.name")
    PremiumUserDto toDto(PremiumUser entity);
}
