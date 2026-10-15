package com.wetube.wetube_service.dto.translation;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranslationResponse {
    @JsonProperty("translationId")   // Node.js trả "translationId"
    private String translationId;

    private String targetLang;
    private String subtitleUrl;
    private String status;
    private List<TranslationSegmentDto> segments;

    // đổi LocalDateTime -> Instant (để parse "2025-09-21T13:50:46.937Z")
    private Instant createdAt;
    private Instant updatedAt;
}

