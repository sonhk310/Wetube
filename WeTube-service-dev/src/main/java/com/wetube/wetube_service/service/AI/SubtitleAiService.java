package com.wetube.wetube_service.service.AI;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wetube.wetube_service.dto.video.SubtitleCue;
import com.wetube.wetube_service.dto.video.SubtitleManageDto;
import com.wetube.wetube_service.service.CloudinaryService;
import com.wetube.wetube_service.service.video.VideoSubtitleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Tạo phụ đề bằng Gemini: lấy audio mp3 của video từ Cloudinary, nhờ Gemini nghe + dịch
 * sang ngôn ngữ đích và lưu các câu vào video_subtitles ở dạng bản nháp (file WebVTT dựng lúc công khai).
 * Chạy nền để không chặn request.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubtitleAiService {

    private static final Map<String, String> LANGUAGE_NAMES = Map.of(
            "vi", "Vietnamese",
            "en", "English",
            "ko", "Korean",
            "ja", "Japanese",
            "zh", "Chinese");

    // Một luồng là đủ: tránh dồn nhiều request Gemini cùng lúc trên máy 1 CPU.
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final GeminiClient geminiClient;
    private final CloudinaryService cloudinaryService;
    private final VideoSubtitleService subtitleService;

    public static boolean isSupported(String lang) {
        return lang != null && LANGUAGE_NAMES.containsKey(lang);
    }

    /**
     * Tạo ngay dòng phụ đề PROCESSING (thay phụ đề cùng ngôn ngữ đang có) rồi chạy AI nền.
     * Xong thì phụ đề thành bản nháp (DRAFT) để chủ video xem, sửa rồi công khai; lỗi thì FAILED.
     */
    public SubtitleManageDto start(UUID videoId, String videoUrl, String targetLang) {
        SubtitleManageDto row = subtitleService.startAi(videoId, targetLang);
        executor.submit(() -> {
            try {
                generate(row.getId(), videoId, videoUrl, targetLang);
            } catch (Exception e) {
                log.error("[SubtitleAI] video {} ({}) failed: {}", videoId, targetLang, e.getMessage(), e);
                subtitleService.failAi(row.getId(), "Không tạo được phụ đề, vui lòng thử lại sau.");
            }
        });
        return row;
    }

    private void generate(UUID subtitleId, UUID videoId, String videoUrl, String targetLang) throws Exception {
        String audioUrl = cloudinaryService.audioUrlFor(videoUrl);
        HttpResponse<byte[]> audio = httpClient.send(
                HttpRequest.newBuilder(URI.create(audioUrl)).timeout(Duration.ofMinutes(3)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
        if (audio.statusCode() != 200) {
            throw new IllegalStateException("Cannot fetch audio from Cloudinary: HTTP " + audio.statusCode());
        }
        log.info("[SubtitleAI] video {}: {} KB audio, translating to {}", videoId, audio.body().length / 1024, targetLang);

        String json = geminiClient.generateJsonFromAudio(audio.body(), prompt(targetLang));
        List<SubtitleCue> cues = toCues(objectMapper.readTree(json));
        if (cues.isEmpty()) {
            log.info("[SubtitleAI] video {}: no speech detected, no subtitle created", videoId);
            subtitleService.failAi(subtitleId, "Không nghe thấy lời nói rõ ràng trong video nên chưa tạo được phụ đề.");
            return;
        }
        subtitleService.completeAi(subtitleId, cues);
        log.info("[SubtitleAI] video {}: subtitle {} draft saved ({} cues)", videoId, targetLang, cues.size());
    }

    private static String prompt(String targetLang) {
        return "Transcribe the speech in this audio and translate it into " + LANGUAGE_NAMES.get(targetLang)
                + ". Return ONLY a JSON array of subtitle cues in time order: "
                + "[{\"start\":\"HH:MM:SS.mmm\",\"end\":\"HH:MM:SS.mmm\",\"original\":\"...\",\"translated\":\"...\"}]. "
                + "Keep each cue under 7 seconds. Only include words that are clearly spoken in the audio; "
                + "never guess or invent dialogue. If there is no clear speech, return [].";
    }

    /** Mảng cue của Gemini → danh sách câu; bỏ câu trống và câu có giờ hỏng. */
    static List<SubtitleCue> toCues(JsonNode cues) {
        List<SubtitleCue> result = new ArrayList<>();
        if (!cues.isArray()) {
            return result;
        }
        for (JsonNode cue : cues) {
            String text = cue.path("translated").asText("").trim();
            if (text.isEmpty()) {
                continue;
            }
            double start;
            double end;
            try {
                start = seconds(cue.path("start").asText());
                end = seconds(cue.path("end").asText());
            } catch (NumberFormatException e) {
                continue;
            }
            if (start < 0 || end <= start) {
                continue;
            }
            String original = cue.path("original").asText("").trim();
            result.add(new SubtitleCue(start, end, text, original.isEmpty() ? null : original));
        }
        return result;
    }

    /** Gemini đôi khi trả "MM:SS.mmm" hoặc "SS.mmm" thay vì "HH:MM:SS.mmm": đổi cả ba ra giây. */
    static double seconds(String t) {
        String[] parts = t.trim().replace(',', '.').split(":");
        double seconds = 0;
        for (String part : parts) {
            seconds = seconds * 60 + Double.parseDouble(part);
        }
        return Math.round(seconds * 1000) / 1000.0;
    }
}
