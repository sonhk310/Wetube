package com.wetube.wetube_service.mapper.video;

import org.mapstruct.Mapper;

import com.wetube.wetube_service.entity.video.VideoTag;
import com.wetube.wetube_service.entity.video.Tag;

@Mapper(componentModel = "spring", uses = TagMapper.class)
public interface VideoTagMapper {
    default Tag toTag(VideoTag vt) { return vt == null ? null : vt.getTag(); }
}

