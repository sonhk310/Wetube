package com.wetube.wetube_service.service.video.impl;

import org.springframework.stereotype.Service;

import com.wetube.wetube_service.dto.video.TagDto;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import com.wetube.wetube_service.service.video.TagService;

@Service
@Transactional
@AllArgsConstructor
public class TagServiceImpl implements TagService {

    @Override
    public TagDto createTag(TagDto tagDto) throws Exception {
        return tagDto;
        
    }
    
}
