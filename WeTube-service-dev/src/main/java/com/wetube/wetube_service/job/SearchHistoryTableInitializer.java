package com.wetube.wetube_service.job;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bảng lịch sử tìm kiếm (không có entity JPA, SearchHistoryService dùng câu SQL thẳng), tạo ở đây
 * nếu chưa có. Mỗi người một dòng cho mỗi từ khoá; tìm lại thì chỉ cập nhật thời gian.
 */
@Slf4j
@Component
@Order(0)
public class SearchHistoryTableInitializer implements CommandLineRunner {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            em.createNativeQuery("""
                    CREATE TABLE IF NOT EXISTS search_history (
                        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        user_id VARCHAR(36) NOT NULL,
                        query VARCHAR(200) NOT NULL,
                        searched_at DATETIME(6) NOT NULL,
                        UNIQUE KEY uk_search_history_user_query (user_id, query),
                        INDEX idx_search_history_user_time (user_id, searched_at)
                    )""").executeUpdate();
        } catch (Exception e) {
            log.error("Không tạo được bảng search_history: {}", e.getMessage());
        }
    }
}
