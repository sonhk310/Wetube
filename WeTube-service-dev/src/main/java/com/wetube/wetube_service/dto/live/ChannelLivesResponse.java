package com.wetube.wetube_service.dto.live;

import java.util.List;

/** Tab Trực tiếp của kênh: buổi đang phát (nếu có) và một trang các buổi đã phát, mới nhất trước. */
public record ChannelLivesResponse(LiveCard current, List<LiveCard> past, long totalPast, boolean hasMore) {
}
