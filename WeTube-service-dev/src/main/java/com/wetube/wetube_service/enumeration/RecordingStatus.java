package com.wetube.wetube_service.enumeration;

/**
 * Bản ghi hình của buổi phát: RECORDING đang ghi → STOPPING đã báo dừng, chờ LiveKit trả file → DONE (đã thành video)
 * hoặc FAILED / SKIPPED (không có video, lý do ở recordingMessage). Cột lưu VARCHAR; null coi như NONE (chưa bật).
 */
public enum RecordingStatus {
    NONE,
    RECORDING,
    STOPPING,
    DONE,
    FAILED,
    SKIPPED
}
