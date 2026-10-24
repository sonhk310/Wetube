package com.wetube.wetube_service.dto.response.analytics;

import java.util.List;

/** Số liệu phân tích của kênh trong một khoảng ngày (giờ VN, "yyyy-MM-dd"); daily đủ mọi ngày, cũ trước. */
public record ChannelAnalyticsDto(String from, String to, String trackingSince, Totals totals, List<Day> daily) {

    public record Totals(long views, long watchSeconds, long subscribers, long comments) {
    }

    public record Day(String date, long views, long watchSeconds, long subscribers, long comments) {
    }
}
