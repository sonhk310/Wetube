package com.wetube.wetube_service.dto.live;

import java.time.LocalDateTime;
import java.util.UUID;

import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.enumeration.RecordingStatus;

/**
 * Một buổi đã phát trong danh sách quản lý ở Studio. replayVideoId có khi bản ghi đã thành video còn tồn tại, kèm
 * replayStatus (chế độ hiển thị: mặc định PRIVATE); recordingStatus STOPPING nghĩa là còn chờ trình duyệt tải bản ghi
 * lên (chưa xoá được). channelBackground: ảnh bìa dự phòng khi buổi không có ảnh thu nhỏ, như thẻ công khai.
 */
public record MyPastLive(
        UUID id,
        String title,
        String thumbnailUrl,
        String channelBackground,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        int peakViewers,
        boolean recordingEnabled,
        RecordingStatus recordingStatus,
        String recordingMessage,
        UUID replayVideoId,
        ActiveStatus replayStatus) {
}
