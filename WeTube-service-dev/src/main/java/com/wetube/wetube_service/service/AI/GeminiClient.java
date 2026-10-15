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

@Slf4j
@Component
public class GeminiClient {

    private final String geminiApiKey;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiClient(@Value("${gemini.api-key}") String geminiApiKey) {
        this.geminiApiKey = geminiApiKey;
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
                .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + geminiApiKey))
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
