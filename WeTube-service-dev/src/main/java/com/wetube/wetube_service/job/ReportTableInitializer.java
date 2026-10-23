package com.wetube.wetube_service.job;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prod chạy với ddl-auto=none nên bảng reports (mới thêm) được tạo ở đây nếu chưa có.
 */
@Slf4j
@Component
@Order(0)
public class ReportTableInitializer implements CommandLineRunner {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            em.createNativeQuery("""
                    CREATE TABLE IF NOT EXISTS reports (
                        id VARCHAR(36) NOT NULL PRIMARY KEY,
                        video_id VARCHAR(36) NOT NULL,
                        reporter_id VARCHAR(36) NOT NULL,
                        reason VARCHAR(50) NOT NULL,
                        details TEXT,
                        status VARCHAR(20) NOT NULL,
                        resolution_note VARCHAR(255),
                        created_at DATETIME(6) NOT NULL,
                        resolved_at DATETIME(6),
                        INDEX idx_reports_status (status),
                        INDEX idx_reports_reporter (reporter_id),
                        INDEX idx_reports_video (video_id)
                    )""").executeUpdate();
        } catch (Exception e) {
            log.error("Không tạo được bảng reports: {}", e.getMessage());
        }
    }
}
