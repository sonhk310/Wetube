package com.wetube.wetube_service.job;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Prod chạy với ddl-auto=none nên thay đổi schema của tính năng chế độ hiển thị / không thích video
 * được áp dụng ở đây, idempotent: nối PRIVATE, UNLISTED, DELETED vào cột ENUM videos.videos_status
 * và tạo bảng video_dislikes nếu chưa có.
 */
@Slf4j
@Component
@Order(0)
public class VideoSchemaInitializer implements CommandLineRunner {

    private static final List<String> REQUIRED_STATUSES = List.of("PRIVATE", "UNLISTED", "DELETED", "DRAFT");

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public void run(String... args) {
        extendStatusEnum();
        createDislikeTable();
    }

    private void extendStatusEnum() {
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery("""
                    SELECT COLUMN_TYPE, IS_NULLABLE FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'videos' AND COLUMN_NAME = 'videos_status'
                    """).getResultList();
            if (rows.isEmpty()) {
                log.warn("Không thấy cột videos.videos_status, bỏ qua mở rộng ENUM");
                return;
            }
            String columnType = String.valueOf(rows.get(0)[0]);
            boolean nullable = "YES".equalsIgnoreCase(String.valueOf(rows.get(0)[1]));
            if (!columnType.toLowerCase().startsWith("enum(")) {
                log.info("videos.videos_status là {} (không phải ENUM), không cần đổi", columnType);
                return;
            }

            // enum('A','B',...) -> danh sách giá trị hiện có, giữ nguyên thứ tự
            List<String> values = new ArrayList<>();
            for (String part : columnType.substring(5, columnType.length() - 1).split(",")) {
                values.add(part.trim().replaceAll("^'|'$", ""));
            }
            boolean changed = false;
            for (String required : REQUIRED_STATUSES) {
                if (!values.contains(required)) {
                    values.add(required);
                    changed = true;
                }
            }
            if (!changed) {
                log.info("videos.videos_status đã có đủ giá trị {}", REQUIRED_STATUSES);
                return;
            }

            StringBuilder list = new StringBuilder();
            for (String v : values) {
                if (list.length() > 0) {
                    list.append(',');
                }
                list.append('\'').append(v.replace("'", "''")).append('\'');
            }
            em.createNativeQuery("ALTER TABLE videos MODIFY COLUMN videos_status ENUM(" + list + ")"
                    + (nullable ? " NULL" : " NOT NULL")).executeUpdate();
            log.info("Đã mở rộng videos.videos_status thành ENUM({})", list);
        } catch (Exception e) {
            log.error("Không mở rộng được cột videos.videos_status: {}", e.getMessage());
        }
    }

    private void createDislikeTable() {
        try {
            em.createNativeQuery("""
                    CREATE TABLE IF NOT EXISTS video_dislikes (
                        id VARCHAR(36) NOT NULL PRIMARY KEY,
                        user_id VARCHAR(36) NOT NULL,
                        video_id VARCHAR(36) NOT NULL,
                        created_at DATETIME(6) NOT NULL,
                        UNIQUE KEY uk_video_dislikes_user_video (user_id, video_id),
                        INDEX idx_video_dislikes_video (video_id)
                    )""").executeUpdate();
        } catch (Exception e) {
            log.error("Không tạo được bảng video_dislikes: {}", e.getMessage());
        }
    }
}
