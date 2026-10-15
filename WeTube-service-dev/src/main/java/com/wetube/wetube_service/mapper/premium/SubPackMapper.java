package com.wetube.wetube_service.mapper.premium;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import com.wetube.wetube_service.dto.SubPacksDto;
import com.wetube.wetube_service.dto.request.SubPackRequestDto;
import com.wetube.wetube_service.entity.premium.SubPack;

@Mapper(componentModel = "spring")
public interface SubPackMapper {
    SubPackMapper INSTANCE = Mappers.getMapper(SubPackMapper.class);

    @Mapping(target = "price", source = "price", qualifiedByName = "longToBigDecimal")
    @Mapping(target = "createdAt", ignore = true) // set trong @AfterMapping
    @Mapping(target = "updatedAt", ignore = true)
    SubPack toEntity(SubPackRequestDto dto);

    @Mapping(target = "price", source = "price", qualifiedByName = "bigDecimalToLong")
    SubPacksDto toDto(SubPack entity);

     @Named("longToBigDecimal")
    default BigDecimal longToBigDecimal(Long value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }

    @Named("bigDecimalToLong")
    default Long bigDecimalToLong(BigDecimal value) {
        return value != null ? value.longValue() : null;
    }

    @Mapping(target = "price", source = "price", qualifiedByName = "longToBigDecimal")
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    void updateEntityFromDto(SubPackRequestDto dto, @MappingTarget SubPack entity);


    @AfterMapping
    default void setTimestampsOnCreate(SubPackRequestDto dto, @MappingTarget SubPack entity) {
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
    }
    

    // Set updatedAt khi update
    @AfterMapping
    default void setTimestampOnUpdate(SubPackRequestDto dto, @MappingTarget SubPack entity) {
        entity.setUpdatedAt(LocalDateTime.now());
    }
    
}
