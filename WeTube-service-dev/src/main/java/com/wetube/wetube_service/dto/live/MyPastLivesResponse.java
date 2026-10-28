package com.wetube.wetube_service.dto.live;

import java.util.List;

/** Các buổi đã phát của kênh người gọi, mới nhất trước (mọi buổi, kể cả buổi không có video xem lại). */
public record MyPastLivesResponse(List<MyPastLive> items, long total, boolean hasMore) {
}
