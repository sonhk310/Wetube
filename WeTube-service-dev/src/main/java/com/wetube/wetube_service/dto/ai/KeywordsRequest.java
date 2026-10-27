package com.wetube.wetube_service.dto.ai;

import lombok.Data;

@Data
public class KeywordsRequest {
    private String title;
    private String description;
    private String tags;
}
