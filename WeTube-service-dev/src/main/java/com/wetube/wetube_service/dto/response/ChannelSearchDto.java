package com.wetube.wetube_service.dto.response;

import java.util.UUID;

/** Một kênh trong kết quả tìm kiếm (bộ lọc "Kênh"). */
public record ChannelSearchDto(UUID id, String name, String picture, String description,
        int totalSubscribers, int totalVideos, String handle) {
}
