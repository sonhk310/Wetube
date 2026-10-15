package com.wetube.wetube_service.service.video;

import java.util.List;
import java.util.UUID;

import com.wetube.wetube_service.dto.video.RecommendVideoDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RecService {
//    List<RecommendVideoDto> recommendVideos (UUID userId, int limit);
    Page<RecommendVideoDto> recommendVideos(UUID userId, Pageable pageable);
    List<RecommendVideoDto> findTopRankedVideos(UUID userId, int poolSize, int topN);
}
