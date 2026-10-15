package com.wetube.wetube_service.dto.request;

import lombok.Data;

@Data
public class CreateSubtitleRequest {
    private String language;
    private String url;
}

