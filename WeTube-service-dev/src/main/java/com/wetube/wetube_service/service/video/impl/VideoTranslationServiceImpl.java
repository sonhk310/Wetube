package com.wetube.wetube_service.service.video.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.wetube.wetube_service.dto.translation.TranslationsResponse;
import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.service.client.TranslationClient;
import com.wetube.wetube_service.service.video.VideoTranslationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VideoTranslationServiceImpl implements VideoTranslationService {
    private final VideoRepository videoRepository;
    private final TranslationClient translationClient;

    @Override
    public TranslationsResponse translateVideo(UUID videoId, String targetLang) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        return translationClient.requestTranslation(videoId, video.getVideoUrl(), targetLang);
    }

    @Override
    public TranslationsResponse getVideoTranslations(UUID videoId) {
        return translationClient.getTranslationsByVideoId(videoId);
    }
}
