package com.wetube.wetube_service.mapper.post;

import com.wetube.wetube_service.dto.post.PollOptionDto;
import com.wetube.wetube_service.entity.post.PollOption;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PollOptionMapper {
    PollOptionDto toDto(PollOption entity);
    PollOption toEntity(PollOptionDto dto);
}


