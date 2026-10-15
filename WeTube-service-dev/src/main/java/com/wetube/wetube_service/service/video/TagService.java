package com.wetube.wetube_service.service.video;

import com.wetube.wetube_service.dto.video.TagDto;

public interface TagService {
    TagDto createTag(TagDto tagDto) throws Exception;
}
