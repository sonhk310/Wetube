package com.wetube.wetube_service.dto.translation;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder    
public class TranslationRequest {
    private UUID videoId;  
    private String videoUrl;
    private String targetLang;
}
