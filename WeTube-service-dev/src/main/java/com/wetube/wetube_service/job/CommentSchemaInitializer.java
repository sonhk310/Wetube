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
        // Chủ đề video (C8) và điểm yêu thích chủ đề
        // Prod chạy ddl-auto=update: Hibernate có thể đã thêm cột (không tạo index), nên index kiểm tra riêng
        addColumn("videos", "topic_id", "VARCHAR(100) NULL");
        addIndex("videos", "idx_videos_topic", "topic_id");
        try {
            tx.executeWithoutResult(s -> em.createNativeQuery("""
                    CREATE TABLE IF NOT EXISTS user_topic (
                        user_id VARCHAR(36) NOT NULL,
                        topic_id VARCHAR(100) NOT NULL,
                        point DOUBLE NOT NULL,
                        created_at DATETIME(6),
                        PRIMARY KEY (user_id, topic_id)
                    )""").executeUpdate());
        } catch (Exception e) {
            log.error("Không tạo được bảng user_topic: {}", e.getMessage());
        }
        // Phát trực tiếp (L1): thông báo LIVE_STARTED trỏ tới buổi phát; chỉ mục cho các bảng do LiveSchemaInitializer tạo
        addColumn("notifications", "live_id", "VARCHAR(36) NULL");
        addIndex("live_streams", "idx_live_streams_status", "status");
        addIndex("live_streams", "idx_live_streams_channel", "channel_id");
        // Ghi hình (L2): cộng dồn số phút đã ghi theo kênh và theo tháng
        addIndex("live_streams", "idx_live_streams_recording", "channel_id, recording_started_at");
        addIndex("live_polls", "idx_live_polls_stream", "stream_id");
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
        // Duyệt / sửa phụ đề (E2): phụ đề cũ đã công khai; AI chạy trong bộ nhớ nên khởi động lại là mất lượt đang chạy
        addColumn("video_subtitles", "status", "VARCHAR(20) NULL");
        addColumn("video_subtitles", "cues", "MEDIUMTEXT NULL");
        addColumn("video_subtitles", "ai_generated", "BIT(1) NULL");
        addColumn("video_subtitles", "error_message", "VARCHAR(500) NULL");
        addColumn("video_subtitles", "updated_at", "DATETIME(6) NULL");
        addIndex("video_subtitles", "idx_video_subtitles_video", "video_id");
        execute("UPDATE video_subtitles SET status = 'PUBLISHED' WHERE status IS NULL", "phụ đề cũ sang PUBLISHED");
        execute("UPDATE video_subtitles SET status = 'FAILED', error_message = 'Máy chủ khởi động lại khi đang tạo phụ đề, vui lòng tạo lại.' "
                + "WHERE status = 'PROCESSING'", "phụ đề đang tạo sang FAILED");
    }

    private void execute(String sql, String what) {
        try {
            tx.executeWithoutResult(s -> em.createNativeQuery(sql).executeUpdate());
        } catch (Exception e) {
            log.error("Không chuyển được {}: {}", what, e.getMessage());
        }
    }

    /** Tạo index nếu chưa có index tên này. */
    private void addIndex(String table, String index, String column) {
        try {
            Number existing = tx.execute(s -> (Number) em.createNativeQuery("""
                    SELECT COUNT(*) FROM information_schema.STATISTICS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = :table AND INDEX_NAME = :index
                    """)
                    .setParameter("table", table)
                    .setParameter("index", index)
                    .getSingleResult());
            if (existing == null || existing.intValue() > 0) {
                return;
            }
            tx.executeWithoutResult(s -> em.createNativeQuery(
                    "CREATE INDEX " + index + " ON " + table + " (" + column + ")").executeUpdate());
            log.info("Đã tạo index {}.{}", table, index);
        } catch (Exception e) {
            log.error("Không tạo được index {}.{}: {}", table, index, e.getMessage());
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
