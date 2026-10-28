package com.wetube.wetube_service.job;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;

/**
 * Tạo các bảng phát trực tiếp nếu chưa có. Production không tự tạo bảng mới từ entity (log: "Table live_streams doesn't
 * exist"), nên làm như các runner khác. Chạy trước CommentSchemaInitializer (Order 1) vì runner đó tạo chỉ mục cho các
 * bảng này. Cột phải khớp entity trong entity/live; chạy lại không làm gì thêm. Mỗi câu lệnh một transaction riêng.
 */
@Slf4j
@Component
@Order(0)
public class LiveSchemaInitializer implements CommandLineRunner {

    @PersistenceContext
    private EntityManager em;

    private final TransactionTemplate tx;

    public LiveSchemaInitializer(PlatformTransactionManager transactionManager) {
        this.tx = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(String... args) {
        create("live_streams", """
                CREATE TABLE IF NOT EXISTS live_streams (
                    id VARCHAR(36) NOT NULL PRIMARY KEY,
                    channel_id VARCHAR(36) NOT NULL,
                    owner_id VARCHAR(36) NOT NULL,
                    title VARCHAR(100) NOT NULL,
                    description TEXT,
                    source VARCHAR(20) NOT NULL,
                    protocol VARCHAR(20),
                    status VARCHAR(20) NOT NULL,
                    thumbnail_url VARCHAR(500),
                    chat_enabled BIT(1) NOT NULL,
                    recording_enabled BIT(1) NOT NULL DEFAULT 0,
                    topic_id VARCHAR(100),
                    recording_status VARCHAR(20),
                    recording_started_at DATETIME(6),
                    recording_stopped_at DATETIME(6),
                    recording_seconds INT NOT NULL DEFAULT 0,
                    recording_url VARCHAR(500),
                    recording_message VARCHAR(200),
                    replay_video_id VARCHAR(36),
                    livekit_room VARCHAR(100),
                    ingress_id VARCHAR(100),
                    ingress_url VARCHAR(500),
                    ingress_key VARCHAR(200),
                    created_at DATETIME(6) NOT NULL,
                    started_at DATETIME(6),
                    ended_at DATETIME(6),
                    last_publisher_at DATETIME(6),
                    current_viewers INT NOT NULL,
                    peak_viewers INT NOT NULL,
                    version BIGINT
                )""");
        create("live_polls", """
                CREATE TABLE IF NOT EXISTS live_polls (
                    id VARCHAR(36) NOT NULL PRIMARY KEY,
                    stream_id VARCHAR(36) NOT NULL,
                    question VARCHAR(200) NOT NULL,
                    options TEXT NOT NULL,
                    is_open BIT(1) NOT NULL,
                    created_at DATETIME(6) NOT NULL,
                    closed_at DATETIME(6)
                )""");
        create("live_poll_votes", """
                CREATE TABLE IF NOT EXISTS live_poll_votes (
                    poll_id VARCHAR(36) NOT NULL,
                    user_id VARCHAR(36) NOT NULL,
                    option_index INT NOT NULL,
                    created_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (poll_id, user_id)
                )""");
        // Bảng live_streams đã có trên production từ L1: CREATE TABLE IF NOT EXISTS không thêm cột cho nó
        addColumn("live_streams", "thumbnail_url", "VARCHAR(500) NULL");
        // Ghi hình (L2): cột mới cho bảng đã có
        addColumn("live_streams", "recording_enabled", "BIT(1) NOT NULL DEFAULT 0");
        addColumn("live_streams", "topic_id", "VARCHAR(100) NULL");
        addColumn("live_streams", "recording_status", "VARCHAR(20) NULL");
        addColumn("live_streams", "recording_started_at", "DATETIME(6) NULL");
        addColumn("live_streams", "recording_stopped_at", "DATETIME(6) NULL");
        addColumn("live_streams", "recording_seconds", "INT NOT NULL DEFAULT 0");
        addColumn("live_streams", "recording_url", "VARCHAR(500) NULL");
        addColumn("live_streams", "recording_message", "VARCHAR(200) NULL");
        addColumn("live_streams", "replay_video_id", "VARCHAR(36) NULL");
    }

    /** Thêm cột nếu chưa có (cột có từ CREATE TABLE ở trên thì bỏ qua). */
    private void addColumn(String table, String column, String definition) {
        try {
            Number existing = tx.execute(s -> (Number) em.createNativeQuery("""
                    SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = :table AND COLUMN_NAME = :column
                    """)
                    .setParameter("table", table)
                    .setParameter("column", column)
                    .getSingleResult());
            if (existing == null || existing.intValue() > 0) {
                return;
            }
            tx.executeWithoutResult(s -> em.createNativeQuery(
                    "ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition).executeUpdate());
            log.info("Đã thêm cột {}.{}", table, column);
        } catch (Exception e) {
            log.error("Không thêm được cột {}.{}: {}", table, column, e.getMessage());
        }
    }

    private void create(String table, String ddl) {
        try {
            tx.executeWithoutResult(s -> em.createNativeQuery(ddl).executeUpdate());
        } catch (Exception e) {
            log.error("Không tạo được bảng {}: {}", table, e.getMessage());
        }
    }
}
