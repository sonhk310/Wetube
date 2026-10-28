package com.wetube.wetube_service.dto.live;

/**
 * Form chuẩn bị hỏi trước khi hiện ô "Ghi hình để xem lại". enabled = máy chủ đã bật ghi hình; secondsLeft = số giây
 * kênh còn được ghi trong tháng (đã lấy mức thấp hơn giữa phần của kênh và phần chung); limitedBy = "CHANNEL" hoặc
 * "TOTAL" khi đã hết, null nếu còn.
 */
public record LiveRecordingInfo(boolean enabled, long secondsLeft, String limitedBy) {
}
