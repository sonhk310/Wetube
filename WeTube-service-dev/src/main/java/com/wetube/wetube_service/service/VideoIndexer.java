package com.wetube.wetube_service.service;

import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.repository.VideoEsRepository;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.search.VideoDocument;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoIndexer {

    private final VideoRepository videoRepo;
    private final VideoEsRepository videoEsRepo;

    // Index 1 video theo ID
    public void indexVideo(UUID videoId) {
        Video v = videoRepo.findByIdWithTags(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        List<String> tagNames = v.getVideoTags().stream()
                .map(vt -> vt.getTag().getName())
                .toList();

        VideoDocument doc = new VideoDocument();
        doc.setId(v.getId().toString());
        doc.setTitle(v.getTitle());
        doc.setDescription(v.getDescription());
        doc.setUserId(v.getUser().getId().toString());
        doc.setTags(v.getVideoTags().stream()
                .map(vt -> vt.getTag().getName())
                .toList());
        doc.setCreatedAt(v.getCreatedAt().atZone(ZoneOffset.UTC).toInstant());
        doc.setThumbnailUrl(v.getThumbnailUrl());
        doc.setVideoUrl(v.getVideoUrl());
        doc.setTotalView(v.getTotalView());
        doc.setDuration(v.getDuration());
        doc.setName(v.getUser().getName());
        doc.setPicture(v.getUser().getPicture());

        videoEsRepo.save(doc);
    }

    // Index toàn bộ video trong DB
    public void reindexAll() {
        videoRepo.findAll().forEach(v -> indexVideo(v.getId()));
    }
}
