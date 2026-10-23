package com.wetube.wetube_service.job;

import com.wetube.wetube_service.repository.premium.SubPackRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Nạp các gói premium mặc định cho database mới.
 * Gói dùng thử có id cố định vì client (PaymentButton) gọi thẳng id này.
 */
@Component
@Order(1)
public class SubPackSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SubPackSeeder.class);

    /** Phải trùng với subPackId trong client: modules/otherservices/premium/ui/layouts/PaymentButton.tsx */
    static final UUID TRIAL_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    private final SubPackRepository subPackRepository;

    @PersistenceContext
    private EntityManager em;

    public SubPackSeeder(SubPackRepository subPackRepository) {
        this.subPackRepository = subPackRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        try {
            // MoMo không nhận số tiền 0, nên gói dùng thử tính 1.000đ
            if (!subPackRepository.existsById(TRIAL_ID)) {
                insert(TRIAL_ID, "Premium Trial", 1_000, 30, "Dùng thử Premium 1 tháng");
                log.info("Đã tạo gói dùng thử mặc định");
            }
            // Các gói thường chỉ nạp khi chưa có gói nào ngoài gói dùng thử
            if (subPackRepository.count() <= 1) {
                insert(UUID.randomUUID(), "Premium 1 tháng", 49_000, 30, "Xem video không quảng cáo trong 1 tháng");
                insert(UUID.randomUUID(), "Premium 3 tháng", 129_000, 90, "Xem video không quảng cáo trong 3 tháng");
                insert(UUID.randomUUID(), "Premium 12 tháng", 490_000, 365, "Xem video không quảng cáo trong 12 tháng");
                log.info("Đã tạo các gói premium mặc định");
            }
        } catch (Exception e) {
            log.error("Không thể nạp gói premium mặc định", e);
        }
    }

    private void insert(UUID id, String name, long price, int durationDays, String description) {
        em.createNativeQuery(
                        "INSERT INTO sub_package (id, name, price, duration_days, description, create_at, update_at) "
                                + "VALUES (?1, ?2, ?3, ?4, ?5, NOW(), NOW())")
                .setParameter(1, id.toString())
                .setParameter(2, name)
                .setParameter(3, price)
                .setParameter(4, durationDays)
                .setParameter(5, description)
                .executeUpdate();
    }
}
