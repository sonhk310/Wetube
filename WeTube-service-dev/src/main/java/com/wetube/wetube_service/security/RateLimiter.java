package com.wetube.wetube_service.security;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.wetube.wetube_service.service.RedisService;

import lombok.RequiredArgsConstructor;

/**
 * Giới hạn số lần gọi trong một khoảng thời gian (cửa sổ cố định). Đếm trên Redis để đúng cả khi
 * deploy lại; Redis không dùng được thì đếm tạm trong bộ nhớ của tiến trình.
 */
@Component
@RequiredArgsConstructor
public class RateLimiter {

    public enum Limit {
        /** Gemini viết mô tả / dịch transcript. */
        AI_TEXT(20, Duration.ofHours(1), "Bạn đã dùng hết lượt AI trong giờ này, vui lòng thử lại sau."),
        /** Gemini tạo phụ đề từ âm thanh video (tốn nhất). */
        AI_SUBTITLE(5, Duration.ofHours(1), "Bạn đã yêu cầu tạo phụ đề AI quá nhiều lần trong giờ này, vui lòng thử lại sau."),
        /** Gửi lượt xem, theo IP. */
        VIEW(120, Duration.ofMinutes(10), "Quá nhiều yêu cầu, vui lòng thử lại sau."),
        COMMENT(10, Duration.ofMinutes(1), "Bạn bình luận quá nhanh, vui lòng chờ một chút."),
        REPORT(20, Duration.ofHours(1), "Bạn đã gửi quá nhiều báo cáo trong giờ này, vui lòng thử lại sau.");

        private final int max;
        private final Duration window;
        private final String message;

        Limit(int max, Duration window, String message) {
            this.max = max;
            this.window = window;
            this.message = message;
        }
    }

    private static final int LOCAL_PURGE_THRESHOLD = 10_000;

    private final RedisService redisService;

    /** Dự phòng khi không có Redis: key -> {hết hạn lúc (ms), số lần}. */
    private final ConcurrentHashMap<String, long[]> localCounters = new ConcurrentHashMap<>();

    /** 429 nếu subject đã vượt giới hạn. */
    public void check(Limit limit, Object subject) {
        if (!tryAcquire(limit, subject)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, limit.message);
        }
    }

    /** Tính thêm 1 lần cho subject; false nếu đã vượt giới hạn. */
    public boolean tryAcquire(Limit limit, Object subject) {
        String key = "rl:" + limit.name().toLowerCase(Locale.ROOT) + ":" + subject;
        Long count = redisService.incrementWithin(key, limit.window);
        if (count == null) {
            count = incrementLocal(key, limit.window);
        }
        return count <= limit.max;
    }

    private long incrementLocal(String key, Duration window) {
        long now = System.currentTimeMillis();
        if (localCounters.size() > LOCAL_PURGE_THRESHOLD) {
            localCounters.values().removeIf(c -> c[0] <= now);
        }
        long[] counter = localCounters.compute(key, (k, c) -> (c == null || c[0] <= now)
                ? new long[] {now + window.toMillis(), 1}
                : new long[] {c[0], c[1] + 1});
        return counter[1];
    }
}
