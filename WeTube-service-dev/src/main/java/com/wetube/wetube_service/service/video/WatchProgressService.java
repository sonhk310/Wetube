package com.wetube.wetube_service.service.video;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Vị trí đang xem của người dùng đã đăng nhập (giây), lưu trong lịch sử xem (cột history_duration).
 * Dùng để phát tiếp từ chỗ dừng và vẽ thanh đỏ trên thumbnail.
 */
public interface WatchProgressService {

    /** Khoá Redis giữ vị trí mới nhất (cũng dùng khi xoá video khỏi lịch sử). */
    static String redisKey(UUID userId, UUID videoId) {
        return "wp:" + userId + ":" + videoId;
    }

    /** Ghi vị trí; lần đầu cũng là lúc video được thêm vào lịch sử. flush = ghi MySQL ngay (khi dừng / rời trang). */
    void record(UUID userId, UUID videoId, float seconds, boolean flush);

    /** Vị trí đã lưu, null nếu chưa xem. */
    Float get(UUID userId, UUID videoId);

    /** Vị trí của nhiều video một lúc (chỉ có video đã xem). */
    Map<UUID, Float> getMany(UUID userId, List<UUID> videoIds);
}
