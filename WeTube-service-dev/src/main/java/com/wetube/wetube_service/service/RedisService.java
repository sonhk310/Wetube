package com.wetube.wetube_service.service;

import java.time.Duration;

/**
 * Truy cập Redis có dự phòng: chưa cấu hình REDIS_URL hoặc Redis lỗi thì các hàm trả null / không làm gì,
 * để nơi gọi tự chạy theo cách cũ. Không bao giờ ném lỗi ra ngoài.
 */
public interface RedisService {

    /** Đã cấu hình và không trong thời gian tạm ngắt sau lỗi. */
    boolean isAvailable();

    /** SET key NX EX ttl: true nếu vừa đặt, false nếu key đã có, null nếu Redis không dùng được. */
    Boolean setIfAbsent(String key, Duration ttl);

    /** INCR key, đặt hạn ttl ở lần tăng đầu tiên (nguyên tử). null nếu Redis không dùng được. */
    Long incrementWithin(String key, Duration ttl);

    String get(String key);

    void set(String key, String value, Duration ttl);

    void delete(String key);
}
