package com.wetube.wetube_service.service.AI;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wetube.wetube_service.dto.request.CreateSubtitleRequest;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Tạo phụ đề bằng Gemini: lấy audio mp3 của video từ Cloudinary, nhờ Gemini nghe + dịch
 * sang ngôn ngữ đích, dựng file WebVTT, upload lên Cloudinary và lưu vào video_subtitles.
 * Chạy nền để không chặn request upload.
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

    public void generateAsync(UUID videoId, String videoUrl, String targetLang) {
        executor.submit(() -> {
            try {
                generate(videoId, videoUrl, targetLang);
            } catch (Exception e) {
                log.error("[SubtitleAI] video {} ({}) failed: {}", videoId, targetLang, e.getMessage(), e);
            }
        });
    }

    private void generate(UUID videoId, String videoUrl, String targetLang) throws Exception {
        String audioUrl = cloudinaryService.audioUrlFor(videoUrl);
        HttpResponse<byte[]> audio = httpClient.send(
                HttpRequest.newBuilder(URI.create(audioUrl)).timeout(Duration.ofMinutes(3)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
        if (audio.statusCode() != 200) {
            throw new IllegalStateException("Cannot fetch audio from Cloudinary: HTTP " + audio.statusCode());
        }
        log.info("[SubtitleAI] video {}: {} KB audio, translating to {}", videoId, audio.body().length / 1024, targetLang);

        String json = geminiClient.generateJsonFromAudio(audio.body(), prompt(targetLang));
        String vtt = toWebVtt(objectMapper.readTree(json));
        if (vtt == null) {
            log.info("[SubtitleAI] video {}: no speech detected, no subtitle created", videoId);
            return;
        }

        String url = cloudinaryService.uploadSubtitle(vtt, "subtitles/" + videoId + "_" + targetLang + ".vtt");
        CreateSubtitleRequest req = new CreateSubtitleRequest();
        req.setLanguage(targetLang);
        req.setUrl(url);
        subtitleService.addSubtitle(videoId, req);
        log.info("[SubtitleAI] video {}: subtitle {} saved", videoId, targetLang);
    }

    private static String prompt(String targetLang) {
        return "Transcribe the speech in this audio and translate it into " + LANGUAGE_NAMES.get(targetLang)
                + ". Return ONLY a JSON array of subtitle cues in time order: "
                + "[{\"start\":\"HH:MM:SS.mmm\",\"end\":\"HH:MM:SS.mmm\",\"original\":\"...\",\"translated\":\"...\"}]. "
                + "Keep each cue under 7 seconds. Only include words that are clearly spoken in the audio; "
                + "never guess or invent dialogue. If there is no clear speech, return [].";
    }

    /** Dựng WebVTT từ mảng cue; null nếu không có cue nào. */
    static String toWebVtt(JsonNode cues) {
        if (!cues.isArray() || cues.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder("WEBVTT\n\n");
        int count = 0;
        for (JsonNode cue : cues) {
            String text = cue.path("translated").asText("").trim();
            if (text.isEmpty()) {
                continue;
            }
            sb.append(normalizeTime(cue.path("start").asText()))
                    .append(" --> ")
                    .append(normalizeTime(cue.path("end").asText()))
                    .append('\n')
                    .append(text.replace("-->", "->"))
                    .append("\n\n");
            count++;
        }
        return count == 0 ? null : sb.toString();
    }

    /** Gemini đôi khi trả "MM:SS.mmm" hoặc "SS.mmm": đưa về "HH:MM:SS.mmm" mà player cần. */
    static String normalizeTime(String t) {
        String[] parts = t.trim().replace(',', '.').split(":");
        double seconds = 0;
        for (String part : parts) {
            seconds = seconds * 60 + Double.parseDouble(part);
        }
        long millis = Math.round(seconds * 1000);
        return String.format("%02d:%02d:%02d.%03d",
                millis / 3_600_000, (millis / 60_000) % 60, (millis / 1000) % 60, millis % 1000);
    }
}
