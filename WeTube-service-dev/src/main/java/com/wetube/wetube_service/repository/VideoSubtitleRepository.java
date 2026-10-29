package com.wetube.wetube_service.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wetube.wetube_service.entity.video.VideoSubtitle;

public interface VideoSubtitleRepository extends JpaRepository<VideoSubtitle, UUID> {
    List<VideoSubtitle> findByVideo_Id(UUID videoId);

    List<VideoSubtitle> findByVideo_IdAndLanguage(UUID videoId, String language);

    Optional<VideoSubtitle> findByIdAndVideo_Id(UUID id, UUID videoId);
}
