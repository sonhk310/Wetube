package com.wetube.wetube_service.dto.ai;

import lombok.Data;

@Data
public class TopicRequest {
    private String title;
    private String description;
    private String tags;
    private String searchKeywords;
}
