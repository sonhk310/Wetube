package com.wetube.wetube_service.job;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;

/**
 * Video xem lại của buổi phát tạo trước đây không có ảnh thu nhỏ khi buổi phát không đặt ảnh (trang chủ kênh, tìm kiếm...
 * hiện ảnh lỗi). Gán cho chúng một khung hình của chính video, như {@code CloudinaryService.videoFrameUrl}. Idempotent;
 * chạy trước khi đồng bộ Elasticsearch ({@code ElasticBootstrapLoader}) để chỉ mục có link mới.
 */
@Slf4j
@Component
@Order(1)
public class LiveReplayThumbnailInitializer implements CommandLineRunner {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            int updated = em.createNativeQuery("""
                    UPDATE videos
                    SET thumbnail_url = REPLACE(REPLACE(video_url, '/upload/', '/upload/so_1,w_1280,h_720,c_fill/'), '.mp4', '.jpg')
                    WHERE (thumbnail_url IS NULL OR thumbnail_url = '')
                      AND video_url LIKE '%/video/upload/%.mp4'
                      AND id IN (SELECT replay_video_id FROM live_streams WHERE replay_video_id IS NOT NULL)
                    """).executeUpdate();
            if (updated > 0) {
                log.info("Đã gán ảnh thu nhỏ (khung hình của video) cho {} video xem lại buổi phát", updated);
            }
        } catch (Exception e) {
            log.error("Không gán được ảnh thu nhỏ cho video xem lại buổi phát: {}", e.getMessage());
        }
    }
}
