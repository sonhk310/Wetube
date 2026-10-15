package com.wetube.wetube_service.service.video.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.wetube.wetube_service.dto.request.CreateSubtitleRequest;
import com.wetube.wetube_service.dto.video.VideoSubtitleDto;
import com.wetube.wetube_service.entity.video.VideoSubtitle;
import com.wetube.wetube_service.exception.ResourceNotFoundException;
import com.wetube.wetube_service.mapper.video.VideoSubtitleMapper;
import com.wetube.wetube_service.repository.VideoSubtitleRepository;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.service.video.VideoSubtitleService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VideoSubtitleServiceImpl implements VideoSubtitleService {

    private final VideoSubtitleRepository repo;
    private final VideoRepository videoRepo;
    private final VideoSubtitleMapper mapper;

    @Override
    public VideoSubtitleDto addSubtitle(UUID videoId, CreateSubtitleRequest req) {
        var video = videoRepo.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video", "id", videoId.toString()));

        var entity = new VideoSubtitle();
        entity.setVideo(video);
        entity.setLanguage(req.getLanguage());
        entity.setSubtitleUrl(req.getUrl());

        repo.save(entity);
        return mapper.toDto(entity);
    }

    @Override
    public List<VideoSubtitleDto> getSubtitlesByVideo(UUID videoId) {
        return repo.findByVideo_Id(videoId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }
}

