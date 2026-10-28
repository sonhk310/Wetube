package com.wetube.wetube_service.dto.live;

import java.time.LocalDateTime;
import java.util.UUID;

import com.wetube.wetube_service.enumeration.RecordingStatus;

/**
 * Một buổi đã phát trong danh sách quản lý ở Studio. replayVideoId có khi bản ghi đã thành video; recordingStatus
 * STOPPING nghĩa là còn chờ trình duyệt tải bản ghi lên (chưa xoá được).
 */
public record MyPastLive(
        UUID id,
        String title,
        String thumbnailUrl,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        int peakViewers,
        boolean recordingEnabled,
        RecordingStatus recordingStatus,
        String recordingMessage,
        UUID replayVideoId) {
}
