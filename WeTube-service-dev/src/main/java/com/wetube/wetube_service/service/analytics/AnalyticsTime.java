package com.wetube.wetube_service.service.analytics;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Số liệu phân tích chia ngày theo giờ Việt Nam; JVM và các cột thời gian chạy UTC. */
public final class AnalyticsTime {
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private AnalyticsTime() {
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    /** Đầu ngày (giờ VN), đổi sang LocalDateTime UTC để so với các cột created_at. */
    public static LocalDateTime startOfDayUtc(LocalDate day) {
        return day.atStartOfDay(ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    /** Ngày (giờ VN) của một thời điểm UTC. */
    public static LocalDate dayOf(LocalDateTime utc) {
        return utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(ZONE).toLocalDate();
    }
}
