package com.wetube.wetube_service.mapper.video;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.wetube.wetube_service.dto.video.TagDto;
import com.wetube.wetube_service.entity.video.Tag;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TagMapper {

    // entity -> dto: tên trùng (id, name, createdAt, count) => MapStruct tự map, KHÔNG cần @Mapping
    TagDto toDto(Tag tag);

    // dto -> entity: bỏ qua các field do JPA/service set
    @Mapping(target = "id",        source = "id")
    @Mapping(target = "name",      source = "name")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "count",     source = "count")
    Tag toEntity(TagDto dto);
}
