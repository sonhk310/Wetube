package com.wetube.wetube_service.job;

import com.wetube.wetube_service.repository.channel.ChannelRepository;
import com.wetube.wetube_service.service.channel.ChannelHandles;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Thêm cột channels.handle / handle_changed_at và index duy nhất nếu chưa có, rồi sinh @handle cho
 * mọi kênh chưa có (kênh tạo trước khi có tính năng này). Chạy lại không làm gì thêm. Mỗi câu lệnh
 * chạy trong transaction riêng để một câu lỗi không làm hỏng cả lúc khởi động.
 */
@Slf4j
@Component
@Order(2)
public class ChannelHandleInitializer implements CommandLineRunner {

    @PersistenceContext
    private EntityManager em;

    private final TransactionTemplate tx;
    private final ChannelRepository channels;

    public ChannelHandleInitializer(PlatformTransactionManager transactionManager, ChannelRepository channels) {
        this.tx = new TransactionTemplate(transactionManager);
        this.channels = channels;
    }

    @Override
    public void run(String... args) {
        addColumn("handle", "VARCHAR(30) NULL");
        addColumn("handle_changed_at", "DATETIME(6) NULL");
        backfill();
        addUniqueIndex();
    }

    private void backfill() {
        try {
            List<Object[]> missing = channels.findIdAndNameWithoutHandle();
            if (missing.isEmpty()) {
                return;
            }
            Set<String> taken = new HashSet<>(channels.findAllHandles());
            int done = 0;
            for (Object[] row : missing) {
                UUID id = row[0] instanceof UUID u ? u : UUID.fromString(String.valueOf(row[0]));
                String handle = ChannelHandles.generate((String) row[1], taken::contains);
                taken.add(handle);
                done += channels.setInitialHandle(id, handle);
            }
            log.info("Đã sinh handle cho {} kênh", done);
        } catch (Exception e) {
            log.error("Không sinh được handle cho kênh cũ: {}", e.getMessage());
        }
    }

    private void addColumn(String column, String definition) {
        try {
            Number existing = tx.execute(s -> (Number) em.createNativeQuery("""
                    SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'channels' AND COLUMN_NAME = :column
                    """).setParameter("column", column).getSingleResult());
            if (existing == null || existing.intValue() > 0) {
                return;
            }
            tx.executeWithoutResult(s -> em.createNativeQuery(
                    "ALTER TABLE channels ADD COLUMN " + column + " " + definition).executeUpdate());
            log.info("Đã thêm cột channels.{}", column);
        } catch (Exception e) {
            log.error("Không thêm được cột channels.{}: {}", column, e.getMessage());
        }
    }

    /** Hibernate (ddl-auto=update) có thể đã tạo ràng buộc duy nhất; chỉ tạo khi chưa có index nào trên cột handle. */
    private void addUniqueIndex() {
        try {
            Number existing = tx.execute(s -> (Number) em.createNativeQuery("""
                    SELECT COUNT(*) FROM information_schema.STATISTICS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'channels' AND COLUMN_NAME = 'handle'
                    """).getSingleResult());
            if (existing == null || existing.intValue() > 0) {
                return;
            }
            tx.executeWithoutResult(s -> em.createNativeQuery(
                    "CREATE UNIQUE INDEX uk_channels_handle ON channels (handle)").executeUpdate());
            log.info("Đã tạo index duy nhất channels.handle");
        } catch (Exception e) {
            log.error("Không tạo được index channels.handle: {}", e.getMessage());
        }
    }
}
