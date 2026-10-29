package com.wetube.wetube_service.service.video;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.wetube.wetube_service.service.RedisService;

import jakarta.annotation.PreDestroy;

/**
 * Làm nóng bản MP3 (phát nền) của video trên Cloudinary. Lần đầu được yêu cầu, Cloudinary tạo MP3 tại chỗ và phát
 * từng khúc không hỗ trợ tua (Accept-Ranges: none): trình duyệt điện thoại tua tới vị trí đang xem rất chậm hoặc
 * không được, nên phát nền im lặng. Chỉ cần có một yêu cầu đầu tiên (kể cả ngắt giữa chừng, Cloudinary vẫn tạo xong
 * và lưu lại), từ lần sau MP3 có Content-Length và tua được. Mỗi video chỉ làm nóng một lần.
 */
@Component
public class AudioWarmer {

    private static final Logger log = LoggerFactory.getLogger(AudioWarmer.class);
    private static final Duration FLAG_TTL = Duration.ofDays(30);
    /** Đọc chừng này byte đầu rồi ngắt kết nối: đủ để Cloudinary bắt đầu và hoàn tất việc tạo tệp. */
    private static final int READ_LIMIT = 64 * 1024;

    private final RedisService redis;
    private final HttpClient http;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "audio-warmer");
        thread.setDaemon(true);
        return thread;
    });
    /** Dự phòng khi không có Redis. */
    private final Set<UUID> warmedLocally = ConcurrentHashMap.newKeySet();

    @Autowired
    public AudioWarmer(RedisService redis) {
        this(redis, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NORMAL).build());
    }

    AudioWarmer(RedisService redis, HttpClient http) {
        this.redis = redis;
        this.http = http;
    }

    private static String key(UUID videoId) {
        return "audio-warm:" + videoId;
    }

    /** Làm nóng nền (không chặn người gọi); video đã làm nóng gần đây thì bỏ qua. */
    public void warm(UUID videoId, String audioUrl) {
        Boolean claimed = redis.setIfAbsent(key(videoId), FLAG_TTL);
        boolean first = claimed != null ? claimed : warmedLocally.add(videoId);
        if (!first) {
            return;
        }
        executor.execute(() -> fetch(videoId, audioUrl));
    }

    void fetch(UUID videoId, String audioUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(audioUrl)).timeout(Duration.ofSeconds(60)).GET().build();
            HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                if (response.statusCode() != 200) {
                    throw new IllegalStateException("HTTP " + response.statusCode());
                }
                body.readNBytes(READ_LIMIT);
            }
            log.info("Đã làm nóng MP3 phát nền của video {}", videoId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            forget(videoId);
        } catch (Exception e) {
            log.warn("Không làm nóng được MP3 của video {}: {}", videoId, e.getMessage());
            forget(videoId);
        }
    }

    /** Lần làm nóng hỏng: bỏ dấu để lần xem sau thử lại. */
    private void forget(UUID videoId) {
        warmedLocally.remove(videoId);
        redis.delete(key(videoId));
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
