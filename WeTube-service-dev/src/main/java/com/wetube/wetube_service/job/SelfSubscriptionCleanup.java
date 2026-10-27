package com.wetube.wetube_service.job;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * Xoá các lượt đăng ký mà chủ kênh tự đăng ký kênh của chính mình (tạo từ lúc nút Đăng ký còn hiện
 * trên kênh của mình; client đã ẩn nút nhưng dòng cũ vẫn nằm trong DB), rồi tính lại
 * channels.total_subscribers của các kênh đó từ số dòng thật. Chạy lại không làm gì thêm.
 */
@Slf4j
@Component
@Order(2)
public class SelfSubscriptionCleanup implements CommandLineRunner {

    private static final String SELF_SUBSCRIPTION = """
            EXISTS (SELECT 1 FROM tiers t JOIN users u ON u.channel_id = t.channel_id
                    WHERE t.id = subscriptions.tier_id AND u.id = subscriptions.user_id)""";

    @PersistenceContext
    private EntityManager em;

    private final TransactionTemplate tx;

    public SelfSubscriptionCleanup(PlatformTransactionManager transactionManager) {
        this.tx = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(String... args) {
        try {
            tx.executeWithoutResult(s -> {
                @SuppressWarnings("unchecked")
                List<Object> channelIds = em.createNativeQuery("""
                        SELECT DISTINCT t.channel_id FROM subscriptions
                        JOIN tiers t ON t.id = subscriptions.tier_id
                        WHERE
                        """ + SELF_SUBSCRIPTION).getResultList();
                if (channelIds.isEmpty()) {
                    return;
                }
                int deleted = em.createNativeQuery("DELETE FROM subscriptions WHERE " + SELF_SUBSCRIPTION)
                        .executeUpdate();
                em.createNativeQuery("""
                        UPDATE channels SET total_subscribers = (
                            SELECT COUNT(*) FROM subscriptions
                            JOIN tiers t ON t.id = subscriptions.tier_id
                            WHERE t.channel_id = channels.id)
                        WHERE id IN (:ids)""")
                        .setParameter("ids", channelIds)
                        .executeUpdate();
                log.info("Đã xoá {} lượt tự đăng ký, tính lại số người đăng ký của {} kênh", deleted, channelIds.size());
            });
        } catch (Exception e) {
            log.error("Không dọn được lượt tự đăng ký: {}", e.getMessage());
        }
    }
}
