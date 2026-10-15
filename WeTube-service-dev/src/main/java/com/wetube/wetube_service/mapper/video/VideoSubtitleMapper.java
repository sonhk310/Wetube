package com.wetube.wetube_service.mapper.video;

import org.mapstruct.Mapper;

import com.wetube.wetube_service.dto.video.VideoSubtitleDto;
import com.wetube.wetube_service.entity.video.VideoSubtitle;

@Mapper(componentModel = "spring")
public interface VideoSubtitleMapper {
    VideoSubtitleDto toDto(VideoSubtitle entity);
}

