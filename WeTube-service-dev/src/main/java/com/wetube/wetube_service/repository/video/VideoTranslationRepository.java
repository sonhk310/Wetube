package com.wetube.wetube_service.repository.video;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wetube.wetube_service.entity.video.VideoTranslation;

@Repository
public interface VideoTranslationRepository extends JpaRepository<VideoTranslation, UUID> {
    Optional<VideoTranslation> findByVideo_IdAndTargetLang(UUID videoId, String targetLang);

}
