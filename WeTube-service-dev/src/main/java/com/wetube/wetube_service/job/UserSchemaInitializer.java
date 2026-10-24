package com.wetube.wetube_service.job;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Thêm cột users.history_paused (tạm dừng lưu lịch sử xem) nếu chưa có, phòng khi prod không chạy
 * ddl-auto=update. Mỗi câu lệnh chạy trong transaction riêng.
 */
@Slf4j
@Component
public class UserSchemaInitializer implements CommandLineRunner {

    @PersistenceContext
    private EntityManager em;

    private final TransactionTemplate tx;

    public UserSchemaInitializer(PlatformTransactionManager transactionManager) {
        this.tx = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(String... args) {
        try {
            Number existing = tx.execute(s -> (Number) em.createNativeQuery("""
                    SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'history_paused'
                    """).getSingleResult());
            if (existing == null || existing.intValue() > 0) {
                return;
            }
            tx.executeWithoutResult(s -> em.createNativeQuery(
                    "ALTER TABLE users ADD COLUMN history_paused BIT(1) NULL").executeUpdate());
            log.info("Đã thêm cột users.history_paused");
        } catch (Exception e) {
            log.error("Không thêm được cột users.history_paused: {}", e.getMessage());
        }
    }
}
