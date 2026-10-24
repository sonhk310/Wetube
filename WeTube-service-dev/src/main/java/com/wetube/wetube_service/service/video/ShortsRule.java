package com.wetube.wetube_service.service.video;

/**
 * Quy tắc Shorts (như YouTube, video tải lên từ 15/10/2024): dài tối đa 180 giây và khung vuông hoặc dọc
 * (rộng không lớn hơn cao). Video ngang luôn là video thường, kể cả khi ngắn.
 */
public final class ShortsRule {

    public static final float MAX_SECONDS = 180f;

    private ShortsRule() {
    }

    public static boolean isShort(Float duration, Integer width, Integer height) {
        return duration != null && duration > 0 && duration <= MAX_SECONDS
                && width != null && height != null && width > 0 && height > 0
                && width <= height;
    }
}
