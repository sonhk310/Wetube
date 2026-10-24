package com.wetube.wetube_service.service.analytics;

import java.time.Duration;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.wetube.wetube_service.service.RedisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Thời gian xem theo ngày: trình phát báo vị trí khoảng mỗi 15 giây, mỗi lần cộng phần đã xem thêm so với
 * lần báo trước (nhớ trong Redis). Tua tới / tua lui thì lần đó không cộng. Redis không dùng được thì không cộng.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WatchTimeTracker {
    /** 15 giây ở tốc độ tối đa 2x, cộng dư cho độ trễ. */
    static final float MAX_DELTA_SECONDS = 35f;
    private static final Duration TTL = Duration.ofDays(1);

    private final RedisService redis;
    private final VideoStatsStore store;

    /** viewerKey: "u:{userId}", "a:{anonId}" hoặc "ip:{IP}" (giống cách đếm lượt xem). */
    public void record(String viewerKey, UUID videoId, UUID ownerId, float position) {
        try {
            if (!redis.isAvailable()) {
                return;
            }
            String key = "wt:" + viewerKey + ":" + videoId;
            Float previous = parse(redis.get(key));
            redis.set(key, Float.toString(position), TTL);
            long delta = deltaSeconds(previous, position);
            if (delta > 0) {
                store.add(videoId, ownerId, AnalyticsTime.today(), 0, delta);
            }
        } catch (Exception e) {
            log.warn("Không ghi được thời gian xem video {}: {}", videoId, e.getMessage());
        }
    }

    static long deltaSeconds(Float previous, float position) {
        if (previous == null) {
            return Math.round(Math.min(Math.max(position, 0f), MAX_DELTA_SECONDS));
        }
        float delta = position - previous;
        return (delta > 0 && delta <= MAX_DELTA_SECONDS) ? Math.round(delta) : 0;
    }

    private static Float parse(String raw) {
        try {
            return raw == null ? null : Float.valueOf(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
