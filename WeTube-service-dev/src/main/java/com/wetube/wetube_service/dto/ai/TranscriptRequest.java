package com.wetube.wetube_service.dto.ai;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TranscriptRequest {
    private String videoId;
    private String lang;
}
