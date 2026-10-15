package com.wetube.wetube_service.dto.video;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoSubtitleDto {
    private String language;
    private String subtitleUrl;
}

