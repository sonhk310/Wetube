package com.wetube.wetube_service.dto.live;

/**
 * Token vào phòng LiveKit. role: PUBLISHER (chủ kênh phát bằng webcam) hoặc VIEWER. full = true khi buổi đã đủ người
 * xem bằng hình: token null, người đó vẫn chat và bình chọn được.
 */
public record LiveTokenResponse(String token, String wsUrl, String role, boolean full) {
}
