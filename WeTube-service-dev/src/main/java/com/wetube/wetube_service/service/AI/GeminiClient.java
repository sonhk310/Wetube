package com.wetube.wetube_service.service.AI;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GeminiClient {

    private final String geminiApiKey;
    private final String model;
    /** Phụ đề AI gửi cả file âm thanh nên tốn token nhất: có model riêng, để đổi model văn bản không làm cạn hạn mức. */
    private final String subtitleModel;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiClient(@Value("${gemini.api-key}") String geminiApiKey,
                        @Value("${gemini.model:gemini-flash-latest}") String model,
                        @Value("${gemini.subtitle-model:gemini-flash-latest}") String subtitleModel) {
        this.geminiApiKey = geminiApiKey;
        this.model = model;
        this.subtitleModel = subtitleModel;
    }

    /**
     * Gửi audio (mp3) kèm prompt, yêu cầu Gemini trả JSON; trả về nguyên văn JSON (không hậu xử lý).
     */
    public String generateJsonFromAudio(byte[] mp3, String prompt) throws Exception {
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(
                        Map.of("inline_data", Map.of(
                                "mime_type", "audio/mpeg",
                                "data", Base64.getEncoder().encodeToString(mp3))),
                        Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "temperature", 0.2));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint(subtitleModel)))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMinutes(5))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            log.error("[Gemini] API error: {}", response.body());
            throw new RuntimeException("Gemini API error: " + response.statusCode());
        }
        JsonNode parts = objectMapper.readTree(response.body())
                .path("candidates").path(0).path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            throw new RuntimeException("No content parts returned from Gemini");
        }
        return parts.get(0).path("text").asText("");
    }

    private String endpoint(String model) {
        return "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + geminiApiKey;
    }

    public String generateText(String prompt) throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                new Object() {
                    public final Object[] contents = new Object[]{
                            new Object() {
                                public final Object[] parts = new Object[]{
                                        new Object() {
                                            public final String text = prompt;
                                        }
                                };
                            }
                    };
                }
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint(model)))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("[Gemini] API error: {}", response.body());
            throw new RuntimeException("Gemini API error: " + response.body());
        }

        JsonNode json = objectMapper.readTree(response.body());
        log.debug("[Gemini] Raw response: {}", json.toPrettyString());

        JsonNode candidates = json.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            throw new RuntimeException("No candidates returned from Gemini: " + response.body());
        }

        JsonNode parts = candidates.get(0).path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            throw new RuntimeException("No content parts returned from Gemini: " + response.body());
        }

        String text = parts.get(0).path("text").asText("");

        text = text.replaceAll("```.*?```", "")
                .replaceAll("\\*+", "")
                .trim();

        log.info("[Gemini] Final output: {}", text);
        return text;
    }
}
