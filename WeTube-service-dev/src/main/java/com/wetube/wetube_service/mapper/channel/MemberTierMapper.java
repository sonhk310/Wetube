package com.wetube.wetube_service.mapper.channel;

import com.wetube.wetube_service.dto.request.MemberTierRequest;
import com.wetube.wetube_service.dto.response.MemberTierResponseDto;
import com.wetube.wetube_service.entity.channel.MembershipTier;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface  MemberTierMapper {
    MemberTierResponseDto toDto(MembershipTier entity);

    @Mapping(target = "id", ignore = true)
    MembershipTier toEntity(MemberTierResponseDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "channel", ignore = true)
    @Mapping(target = "subscriptions", ignore = true)
    void updateEntity(MemberTierResponseDto dto, @MappingTarget MembershipTier entity);

    // Request -> DTO
    @Mapping(target = "defaultTier", ignore = true) // Always false
    MemberTierResponseDto requestToDto(MemberTierRequest request);
}
