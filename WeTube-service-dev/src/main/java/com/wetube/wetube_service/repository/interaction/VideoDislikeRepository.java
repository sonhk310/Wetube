package com.wetube.wetube_service.repository.interaction;

import com.wetube.wetube_service.entity.interaction.VideoDislike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VideoDislikeRepository extends JpaRepository<VideoDislike, UUID> {

    boolean existsByUserIdAndVideoId(UUID userId, UUID videoId);

    long deleteByUserIdAndVideoId(UUID userId, UUID videoId);
}
