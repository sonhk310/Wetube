package com.wetube.wetube_service.service.video;

import java.util.UUID;


import com.wetube.wetube_service.dto.translation.TranslationsResponse;


public interface VideoTranslationService {
    TranslationsResponse translateVideo(UUID videoId, String targetLang);  // chỉ một bản dịch
    TranslationsResponse getVideoTranslations(UUID videoId);              // danh sách bản dịch
}
