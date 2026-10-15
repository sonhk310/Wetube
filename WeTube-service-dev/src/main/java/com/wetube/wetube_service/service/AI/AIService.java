package com.wetube.wetube_service.service.AI;

import org.springframework.stereotype.Service;

@Service
public class AIService {

    private final GeminiClient geminiClient;

    public AIService(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    public String generateVideoDescription(String title, String tags) {
        String prompt = String.format(
                "Write an engaging, SEO-friendly description for a video with title: \"%s\" " +
                        "and tags: %s. Keep it natural, concise, and attractive for viewers.",
                title, tags
        );
        try {
            return geminiClient.generateText(prompt);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate description", e);
        }
    }

    public String generateTranscript(String transcript, String language) {
        String prompt = String.format(
                "Here is a transcript of a video:\n\n%s\n\n" +
                        "Translate this transcript into %s, keeping the dialogue structure intact.",
                transcript, language
        );
        try {
            return geminiClient.generateText(prompt);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate transcript", e);
        }
    }
}
