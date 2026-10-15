package com.wetube.wetube_service.dto.translation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranslationSegmentDto {
    private int index;
    private double start;
    private double end;
    private String originalText;
    private String translatedText;
}

