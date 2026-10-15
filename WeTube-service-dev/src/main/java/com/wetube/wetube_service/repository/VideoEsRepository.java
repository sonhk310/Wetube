package com.wetube.wetube_service.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.wetube.wetube_service.search.VideoDocument;

public interface VideoEsRepository extends ElasticsearchRepository<VideoDocument, String> {
    
}
