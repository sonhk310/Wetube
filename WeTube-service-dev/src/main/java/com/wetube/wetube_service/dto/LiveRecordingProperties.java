package com.wetube.wetube_service.dto;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * Ghi hình buổi phát (biến môi trường LIVE_RECORDING_*). Trình duyệt của chủ kênh ghi rồi tải lên Cloudinary nên không
 * cần kho riêng: bật sẵn, đặt LIVE_RECORDING_ENABLED=false để tắt (form không hiện ô ghi hình, máy chủ từ chối ghi).
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "live.recording")
public class LiveRecordingProperties {
    private boolean enabled = true;
    /** Mỗi kênh được ghi tối đa chừng này phút mỗi tháng (UTC). */
    private int channelMinutes = 15;
    /** Cả WeTube ghi tối đa chừng này phút mỗi tháng: giữ phần ghi hình ở mức nhỏ trong credit Cloudinary. */
    private int totalMinutes = 55;
}
