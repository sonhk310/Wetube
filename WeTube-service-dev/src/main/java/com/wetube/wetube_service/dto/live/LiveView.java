package com.wetube.wetube_service.dto.live;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.wetube.wetube_service.enumeration.LiveProtocol;
import com.wetube.wetube_service.enumeration.LiveSource;
import com.wetube.wetube_service.enumeration.LiveStatus;

import lombok.Builder;

/**
 * Một buổi phát. ingress chỉ có khi người gọi là chủ kênh; poll là Poll đang mở (hoặc vừa đóng trong 60 giây);
 * polls là mọi Poll của buổi, chỉ khi buổi đã kết thúc.
 */
@Builder
public record LiveView(
        UUID id,
        UUID channelId,
        String channelName,
        String channelPicture,
        String channelHandle,
        UUID ownerId,
        String title,
        String description,
        LiveStatus status,
        LiveSource source,
        LiveProtocol protocol,
        String thumbnailUrl,
        boolean chatEnabled,
        int currentViewers,
        int peakViewers,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        IngressView ingress,
        PollView poll,
        List<PollView> polls) {
}
