package com.wetube.wetube_service.dto.live;

import java.time.LocalDateTime;
import java.util.UUID;

import com.wetube.wetube_service.enumeration.LiveStatus;

/** Thẻ buổi phát ở trang /live, chip Trực tiếp và tab Trực tiếp của kênh. */
public record LiveCard(
        UUID id,
        String title,
        UUID channelId,
        String channelName,
        String channelPicture,
        String channelBackground,
        String thumbnailUrl,
        int viewers,
        int peakViewers,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        LiveStatus status) {
}
