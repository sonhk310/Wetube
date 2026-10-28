package com.wetube.wetube_service.dto;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * Ghi hình buổi phát (biến môi trường LIVE_RECORDING_*). Không đặt LIVE_RECORDING_ENABLED thì tính năng tắt hẳn: form
 * không hiện ô ghi hình và máy chủ từ chối yêu cầu ghi.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "live.recording")
public class LiveRecordingProperties {
    private boolean enabled;
    /** Kho tương thích S3 (Cloudflare R2): https://&lt;account-id&gt;.r2.cloudflarestorage.com */
    private String s3Endpoint;
    private String s3Bucket;
    private String s3AccessKey;
    private String s3Secret;
    /** Địa chỉ công khai của bucket, không có dấu / ở cuối; link video = publicUrl + "/" + đường dẫn file. */
    private String publicUrl;
    /** Mỗi kênh được ghi tối đa chừng này phút mỗi tháng (UTC). */
    private int channelMinutes = 15;
    /** Cả WeTube ghi tối đa chừng này phút mỗi tháng: chừa chỗ cho hạn mức 60 phút của gói free LiveKit. */
    private int totalMinutes = 55;

    /** Bật và đủ thông tin kho lưu trữ. */
    public boolean isAvailable() {
        return enabled && filled(s3Endpoint) && filled(s3Bucket) && filled(s3AccessKey) && filled(s3Secret)
                && filled(publicUrl);
    }

    private static boolean filled(String value) {
        return value != null && !value.isBlank();
    }
}
