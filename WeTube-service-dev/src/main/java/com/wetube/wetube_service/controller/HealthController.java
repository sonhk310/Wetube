package com.wetube.wetube_service.controller;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wetube.wetube_service.search.VideoDocument;

@RestController
public class HealthController {

    private final ElasticsearchOperations operations;

    public HealthController(ElasticsearchOperations operations) {
        this.operations = operations;
    }

    @GetMapping("/es/health")
    public String health() {
        return operations.indexOps(VideoDocument.class).exists() ? "Connected ✅" : "Not Connected ❌";
    }
}

