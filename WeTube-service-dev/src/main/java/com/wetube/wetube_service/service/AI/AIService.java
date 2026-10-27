package com.wetube.wetube_service.service.AI;

import com.wetube.wetube_service.utility.SearchKeywords;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
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

    /**
     * Gợi ý từ khóa tìm kiếm cho video (S2): chủ video xem và sửa trước khi lưu. Gemini lỗi thì trả rỗng,
     * việc đăng video không phụ thuộc vào bước này.
     */
    public String suggestSearchKeywords(String title, String description, String tags) {
        String prompt = String.format(
                "A video has this title: \"%s\"\nDescription: \"%s\"\nTags: %s\n\n" +
                        "List 10 to 15 short search keywords people might type to find this video. Include: " +
                        "the title translated into Vietnamese and English, the original name written in Latin letters " +
                        "(romaji for Japanese, pinyin for Chinese), versions without Vietnamese diacritics, and other " +
                        "common names. Use only what the title, description and tags support; do not invent names " +
                        "or facts. Answer with one line of keywords separated by commas and nothing else.",
                title == null ? "" : title, description == null ? "" : description, tags == null ? "" : tags
        );
        try {
            String cleaned = SearchKeywords.clean(geminiClient.generateText(prompt));
            if (cleaned == null) {
                return "";
            }
            // Bỏ từ trùng với thẻ hoặc cả tiêu đề (người xem đã tìm được bằng chúng)
            String lowerTitle = title == null ? "" : title.trim().toLowerCase(Locale.ROOT);
            String lowerTags = tags == null ? "" : tags.toLowerCase(Locale.ROOT);
            return Arrays.stream(cleaned.split(","))
                    .filter(k -> !k.equals(lowerTitle))
                    .filter(k -> !Arrays.asList(lowerTags.split("[\\s#,]+")).contains(k))
                    .collect(Collectors.joining(","));
        } catch (Exception e) {
            log.warn("[Gemini] Không gợi ý được từ khóa tìm kiếm: {}", e.getMessage());
            return "";
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
