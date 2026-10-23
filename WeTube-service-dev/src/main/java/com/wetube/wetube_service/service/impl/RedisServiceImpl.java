package com.wetube.wetube_service.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import com.wetube.wetube_service.service.RedisService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RedisServiceImpl implements RedisService {

    /** Sau một lỗi, bỏ qua Redis trong khoảng này để request không phải chờ timeout liên tục. */
    private static final long PAUSE_AFTER_ERROR_MILLIS = 30_000;

    private static final RedisScript<Long> INCREMENT_WITH_TTL = new DefaultRedisScript<>(
            "local c = redis.call('INCR', KEYS[1]) "
                    + "if c == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end "
                    + "return c",
            Long.class);

    private final StringRedisTemplate redis;
    private final boolean configured;
    private volatile long pausedUntil = 0;

    public RedisServiceImpl(StringRedisTemplate redis, @Value("${REDIS_URL:}") String url) {
        this.redis = redis;
        this.configured = url != null && !url.isBlank();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void checkConnection() {
        if (!configured) {
            log.warn("Chưa cấu hình REDIS_URL: chạy không có Redis (lượt xem, giới hạn tần suất dùng bộ nhớ trong)");
            return;
        }
        String pong = call("PING", "-", () -> redis.execute((RedisCallback<String>) RedisConnection::ping));
        if (pong != null) {
            log.info("Kết nối Redis OK ({})", pong);
        }
    }

    @Override
    public boolean isAvailable() {
        return configured && System.currentTimeMillis() >= pausedUntil;
    }

    @Override
    public Boolean setIfAbsent(String key, Duration ttl) {
        return call("SETNX", key, () -> redis.opsForValue().setIfAbsent(key, "1", ttl));
    }

    @Override
    public Long incrementWithin(String key, Duration ttl) {
        return call("INCR", key,
                () -> redis.execute(INCREMENT_WITH_TTL, List.of(key), String.valueOf(ttl.toMillis())));
    }

    @Override
    public String get(String key) {
        return call("GET", key, () -> redis.opsForValue().get(key));
    }

    @Override
    public List<String> multiGet(List<String> keys) {
        if (keys.isEmpty()) {
            return List.of();
        }
        return call("MGET", keys.size() + " keys", () -> redis.opsForValue().multiGet(keys));
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        call("SET", key, () -> {
            redis.opsForValue().set(key, value, ttl);
            return null;
        });
    }

    @Override
    public void delete(String key) {
        call("DEL", key, () -> redis.delete(key));
    }

    private <T> T call(String op, String key, Supplier<T> action) {
        if (!isAvailable()) {
            return null;
        }
        try {
            return action.get();
        } catch (RuntimeException e) {
            pausedUntil = System.currentTimeMillis() + PAUSE_AFTER_ERROR_MILLIS;
            log.warn("Redis lỗi khi {} {}: {}. Tạm chạy không có Redis trong {} giây",
                    op, key, e.getMessage(), PAUSE_AFTER_ERROR_MILLIS / 1000);
            return null;
        }
    }
}
