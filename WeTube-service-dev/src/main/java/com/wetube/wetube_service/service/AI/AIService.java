package com.wetube.wetube_service.service.AI;

import com.wetube.wetube_service.service.video.TopicCatalog;
import com.wetube.wetube_service.utility.SearchKeywords;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AIService {

    private final GeminiClient geminiClient;
    private final TopicCatalog topicCatalog;

    public AIService(GeminiClient geminiClient, TopicCatalog topicCatalog) {
        this.geminiClient = geminiClient;
        this.topicCatalog = topicCatalog;
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

    /** Nghỉ giữa hai lần gọi Gemini khi lần đầu lỗi (Gemini hay trả 503 "high demand" thoáng qua). */
    long retryPauseMillis = 1500;

    /**
     * Gợi ý từ khóa tìm kiếm cho video (S2): chủ video xem và sửa trước khi lưu. Gemini lỗi thì thử lại một lần,
     * vẫn lỗi thì trả 503 kèm thông báo (khác với "không có gợi ý nào" là danh sách rỗng); việc đăng video
     * không phụ thuộc vào bước này.
     */
    public String suggestSearchKeywords(String title, String description, String tags) {
        String prompt = String.format(
                "A video has this title: \"%s\"\nDescription: \"%s\"\nTags: %s\n\n" +
                        "List 10 to 15 short search keywords people might type to find this video. Include: " +
                        "the title translated into Vietnamese and English, the original name written in Latin letters " +
                        "(romaji for Japanese, pinyin for Chinese) and other common names. Search ignores diacritics, so " +
                        "never list the same keyword twice with and without diacritics. Use only what the title, " +
                        "description and tags support; do not invent names or facts. Answer with one line of " +
                        "keywords separated by commas and nothing else.",
                title == null ? "" : title, description == null ? "" : description, tags == null ? "" : tags
        );
        String answer;
        try {
            answer = generateWithRetry(prompt);
        } catch (Exception e) {
            log.warn("[Gemini] Không gợi ý được từ khóa tìm kiếm: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI đang quá tải, vui lòng thử lại sau ít phút.");
        }
        String cleaned = SearchKeywords.clean(answer);
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
    }

    /**
     * Gợi ý một chủ đề trong topics.json (C8), chủ video đổi được trước khi lưu. Danh sách quá dài để gửi một lần nên
     * hỏi hai bước: chọn nhóm, rồi chọn chủ đề con trong nhóm (hoặc cả nhóm). Nhóm lạ: không gợi ý (""); chủ đề con
     * lạ: trả cả nhóm. Gemini lỗi thì như gợi ý từ khóa: thử lại một lần rồi 503.
     */
    public String suggestTopic(String title, String description, String tags, String searchKeywords) {
        String video = String.format("A video has this title: \"%s\"\nDescription: \"%s\"\nTags: %s\nSearch keywords: %s\n\n",
                title == null ? "" : title, description == null ? "" : description,
                tags == null ? "" : tags, searchKeywords == null ? "" : searchKeywords);
        StringBuilder groups = new StringBuilder();
        for (TopicCatalog.Group g : topicCatalog.groups()) {
            groups.append(g.id()).append(": ").append(g.name()).append('\n');
        }
        String groupId = pickId(video + "Which one of these topic groups fits the video best?\n" + groups
                + "\nAnswer with the id before the colon and nothing else.");
        TopicCatalog.Group group = topicCatalog.group(groupId);
        if (group == null) {
            log.info("[Gemini] Nhóm chủ đề không có trong danh sách: {}", groupId);
            return "";
        }
        StringBuilder topics = new StringBuilder(group.id()).append(": ").append(group.name()).append(" (general)\n");
        for (TopicCatalog.Topic t : group.topics()) {
            topics.append(t.id()).append(": ").append(t.name()).append('\n');
        }
        String topicId = pickId(video + "Which one of these topics fits the video best? Choose the general one if "
                + "no narrower topic fits.\n" + topics + "\nAnswer with the id before the colon and nothing else.");
        return topicCatalog.exists(topicId) && topicId.startsWith(group.id() + ".") ? topicId : group.id();
    }

    private String pickId(String prompt) {
        try {
            String answer = generateWithRetry(prompt);
            // Bỏ dấu nháy / dấu chấm / dòng thừa Gemini hay thêm
            return answer == null ? "" : answer.trim().split("\\s+")[0].replaceAll("[`'\".,:]+$|^[`'\"]+", "");
        } catch (Exception e) {
            log.warn("[Gemini] Không gợi ý được chủ đề: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI đang quá tải, vui lòng thử lại sau ít phút.");
        }
    }

    private String generateWithRetry(String prompt) throws Exception {
        try {
            return geminiClient.generateText(prompt);
        } catch (Exception first) {
            log.info("[Gemini] Lần đầu lỗi, thử lại một lần: {}", first.getMessage());
            Thread.sleep(retryPauseMillis);
            return geminiClient.generateText(prompt);
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
