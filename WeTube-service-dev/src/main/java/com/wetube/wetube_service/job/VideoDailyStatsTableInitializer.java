package com.wetube.wetube_service.job;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bảng số liệu theo ngày cho trang Số liệu phân tích (không có entity JPA, VideoStatsStore dùng câu SQL thẳng),
 * tạo ở đây nếu chưa có. Mỗi video một dòng mỗi ngày (giờ VN).
 */
@Slf4j
@Component
@Order(0)
public class VideoDailyStatsTableInitializer implements CommandLineRunner {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            em.createNativeQuery("""
                    CREATE TABLE IF NOT EXISTS video_daily_stats (
                        video_id VARCHAR(36) NOT NULL,
                        day DATE NOT NULL,
                        owner_id VARCHAR(36) NOT NULL,
                        views INT NOT NULL DEFAULT 0,
                        watch_seconds BIGINT NOT NULL DEFAULT 0,
                        PRIMARY KEY (video_id, day),
                        INDEX idx_vds_owner_day (owner_id, day)
                    )""").executeUpdate();
        } catch (Exception e) {
            log.error("Không tạo được bảng video_daily_stats: {}", e.getMessage());
        }
    }
}
