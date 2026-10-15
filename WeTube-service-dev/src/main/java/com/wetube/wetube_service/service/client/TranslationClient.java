package com.wetube.wetube_service.service.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.wetube.wetube_service.dto.translation.TranslationRequest;
import com.wetube.wetube_service.dto.translation.TranslationsResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TranslationClient {
    private final RestTemplate restTemplate;

    @Value("${translator.service.url}")
    private String translatorServiceBaseUrl;

    /**
     * Gửi yêu cầu dịch video (POST /api/translate/url)
     */
    public TranslationsResponse requestTranslation(UUID videoId, String videoUrl, String targetLang) {
        TranslationRequest request = TranslationRequest.builder()
                .videoId(videoId)
                .videoUrl(videoUrl)
                .targetLang(targetLang)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<TranslationRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<TranslationsResponse> response =
                restTemplate.exchange(
                        translatorServiceBaseUrl + "/url",
                        HttpMethod.POST,
                        entity,
                        TranslationsResponse.class
                );

        return response.getBody();
    }

    /**
     * Lấy toàn bộ bản dịch của một video (GET /api/translate/{videoId})
     */
    public TranslationsResponse getTranslationsByVideoId(UUID videoId) {
        String url = translatorServiceBaseUrl + "/" + videoId;
          String raw = restTemplate.getForObject(url, String.class);
    System.out.println("RAW TRANSLATION JSON: " + raw);
        return restTemplate.getForObject(url, TranslationsResponse.class);
    }

    /**
     * Lấy chi tiết một bản dịch theo translationId (GET /api/translate/detail/{id})
     */
    public TranslationsResponse getTranslationById(String translationId) {
        String url = translatorServiceBaseUrl + "/detail/" + translationId;
        return restTemplate.getForObject(url, TranslationsResponse.class);
    }
}
