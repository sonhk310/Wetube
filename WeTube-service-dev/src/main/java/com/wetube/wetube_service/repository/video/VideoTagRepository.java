package com.wetube.wetube_service.repository.video;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import com.wetube.wetube_service.entity.video.VideoTag;

public interface VideoTagRepository extends JpaRepository<VideoTag, UUID> {
    @EntityGraph(attributePaths = "tag")
    List<VideoTag> findByVideo_Id(UUID videoId);

    boolean existsByVideo_IdAndTag_Id(@Param("videoId") UUID videoId,
                                      @Param("tagId")   UUID tagId);
}
