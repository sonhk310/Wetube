package com.wetube.wetube_service.repository.video;

import com.wetube.wetube_service.search.VideoDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface VideoSearchRepository extends ElasticsearchRepository<VideoDocument, String> {
    // Search gần đúng (full-text)
    Page<VideoDocument> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    // Search chính xác (exact match)
    VideoDocument findByTitle(String title);
}

