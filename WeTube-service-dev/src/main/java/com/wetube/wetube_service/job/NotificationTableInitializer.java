package com.wetube.wetube_service.job;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;

/**
 * Prod chạy với ddl-auto=none nên bảng notifications (mới thêm) được tạo ở đây nếu chưa có.
 */
@Slf4j
@Component
@Order(0)
public class NotificationTableInitializer implements CommandLineRunner {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            em.createNativeQuery("""
                    CREATE TABLE IF NOT EXISTS notifications (
                        id VARCHAR(36) NOT NULL PRIMARY KEY,
                        recipient_id VARCHAR(36) NOT NULL,
                        type VARCHAR(30) NOT NULL,
                        actor_id VARCHAR(36),
                        video_id VARCHAR(36),
                        comment_id VARCHAR(36),
                        text VARCHAR(255),
                        is_read BIT(1) NOT NULL DEFAULT 0,
                        created_at DATETIME(6) NOT NULL,
                        INDEX idx_notifications_recipient (recipient_id, created_at),
                        INDEX idx_notifications_unread (recipient_id, is_read),
                        INDEX idx_notifications_video (video_id, type)
                    )""").executeUpdate();
        } catch (Exception e) {
            log.error("Không tạo được bảng notifications: {}", e.getMessage());
        }
    }
}
