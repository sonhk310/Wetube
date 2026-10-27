package com.wetube.wetube_service.job;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Prod chạy với ddl-auto=none nên các cột cho ghim / tim bình luận và báo cáo bình luận được thêm ở
 * đây nếu chưa có. Chạy sau ReportTableInitializer (bảng reports phải có trước). Mỗi câu lệnh chạy
 * trong transaction riêng để một câu lỗi không làm hỏng cả lúc khởi động.
 */
@Slf4j
@Component
@Order(1)
public class CommentSchemaInitializer implements CommandLineRunner {

    @PersistenceContext
    private EntityManager em;

    private final TransactionTemplate tx;

    public CommentSchemaInitializer(PlatformTransactionManager transactionManager) {
        this.tx = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(String... args) {
        addColumn("comments", "pinned", "BIT(1) NOT NULL DEFAULT 0");
        addColumn("comments", "hearted", "BIT(1) NOT NULL DEFAULT 0");
        if (addColumn("reports", "comment_id", "VARCHAR(36) NULL")) {
            try {
                tx.executeWithoutResult(s -> em.createNativeQuery(
                        "CREATE INDEX idx_reports_comment ON reports (comment_id)").executeUpdate());
            } catch (Exception e) {
                log.error("Không tạo được index reports.comment_id: {}", e.getMessage());
            }
        }
        // "Bỏ ẩn" video / kênh trả video về đúng trạng thái trước khi admin ẩn
        addColumn("videos", "status_before_hidden", "VARCHAR(20) NULL");
        addColumn("videos", "hidden_with_channel", "BIT(1) NULL");
        // Từ khóa tìm kiếm do chủ video duyệt (Gemini gợi ý)
        addColumn("videos", "search_keywords", "TEXT NULL");
        // Báo cáo kênh: có channel_id, không có video_id
        if (addColumn("reports", "channel_id", "VARCHAR(36) NULL")) {
            try {
                tx.executeWithoutResult(s -> em.createNativeQuery(
                        "CREATE INDEX idx_reports_channel ON reports (channel_id)").executeUpdate());
            } catch (Exception e) {
                log.error("Không tạo được index reports.channel_id: {}", e.getMessage());
            }
        }
        try {
            tx.executeWithoutResult(s -> em.createNativeQuery(
                    "ALTER TABLE reports MODIFY video_id VARCHAR(36) NULL").executeUpdate());
        } catch (Exception e) {
            log.error("Không đổi được reports.video_id sang NULL: {}", e.getMessage());
        }
    }

    /** Thêm cột nếu chưa có; true nếu vừa thêm. */
    private boolean addColumn(String table, String column, String definition) {
        try {
            Number existing = tx.execute(s -> (Number) em.createNativeQuery("""
                    SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = :table AND COLUMN_NAME = :column
                    """)
                    .setParameter("table", table)
                    .setParameter("column", column)
                    .getSingleResult());
            if (existing == null || existing.intValue() > 0) {
                return false;
            }
            tx.executeWithoutResult(s -> em.createNativeQuery(
                    "ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition).executeUpdate());
            log.info("Đã thêm cột {}.{}", table, column);
            return true;
        } catch (Exception e) {
            log.error("Không thêm được cột {}.{}: {}", table, column, e.getMessage());
            return false;
        }
    }
}
