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

import java.util.List;
import java.util.UUID;

/**
 * Nạp các gói premium mặc định (cùng dữ liệu với resources/data.sql) cho database mới.
 * Id của gói đầu tiên phải trùng với subPackId trong client:
 * modules/otherservices/premium/ui/layouts/PaymentButton.tsx
 */
@Component
@Order(1)
public class SubPackSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SubPackSeeder.class);

    private record Pack(String id, String name, long price, int durationDays, String description) {}

    private static final List<Pack> DEFAULT_PACKS = List.of(
            new Pack("550e8400-e29b-41d4-a716-446655440000", "Basic Plan", 99_000, 30, "Gói cơ bản cho người mới bắt đầu"),
            new Pack("550e8400-e29b-41d4-a716-446655440001", "Standard Plan", 249_000, 90, "Gói tiêu chuẩn, đầy đủ tính năng phổ biến"),
            new Pack("550e8400-e29b-41d4-a716-446655440002", "Premium Plan", 499_000, 180, "Gói cao cấp, nhiều tiện ích nâng cao"),
            new Pack("550e8400-e29b-41d4-a716-446655440003", "VIP Plan", 899_000, 365, "Gói VIP cho người dùng chuyên nghiệp"));

    private final SubPackRepository subPackRepository;

    @PersistenceContext
    private EntityManager em;

    public SubPackSeeder(SubPackRepository subPackRepository) {
        this.subPackRepository = subPackRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        removeTemporaryPacks();
        for (Pack pack : DEFAULT_PACKS) {
            try {
                if (subPackRepository.existsById(UUID.fromString(pack.id()))) {
                    continue;
                }
                em.createNativeQuery(
                                "INSERT INTO sub_package (id, name, price, duration_days, description, create_at, update_at) "
                                        + "VALUES (?1, ?2, ?3, ?4, ?5, NOW(), NOW())")
                        .setParameter(1, pack.id())
                        .setParameter(2, pack.name())
                        .setParameter(3, pack.price())
                        .setParameter(4, pack.durationDays())
                        .setParameter(5, pack.description())
                        .executeUpdate();
                log.info("Đã tạo gói premium mặc định {}", pack.name());
            } catch (Exception e) {
                log.error("Không thể tạo gói premium mặc định {}", pack.name(), e);
            }
        }
    }

    /**
     * Bản triển khai đầu tiên từng nạp nhầm 4 gói tạm (gói dùng thử 1.000đ và 3 gói theo tháng).
     * Đưa gói dùng thử về Basic Plan và xoá 3 gói còn lại nếu chưa có thanh toán nào dùng tới.
     */
    private void removeTemporaryPacks() {
        try {
            Pack basic = DEFAULT_PACKS.get(0);
            em.createNativeQuery(
                            "UPDATE sub_package SET name = ?1, price = ?2, duration_days = ?3, description = ?4, update_at = NOW() "
                                    + "WHERE id = ?5 AND name = 'Premium Trial'")
                    .setParameter(1, basic.name())
                    .setParameter(2, basic.price())
                    .setParameter(3, basic.durationDays())
                    .setParameter(4, basic.description())
                    .setParameter(5, basic.id())
                    .executeUpdate();
            em.createNativeQuery(
                            "DELETE FROM sub_package WHERE name IN ('Premium 1 tháng', 'Premium 3 tháng', 'Premium 12 tháng') "
                                    + "AND NOT EXISTS (SELECT 1 FROM payment_momo p WHERE p.sub_package_id = sub_package.id) "
                                    + "AND NOT EXISTS (SELECT 1 FROM premium_user u WHERE u.sub_package_id = sub_package.id)")
                    .executeUpdate();
        } catch (Exception e) {
            log.error("Không thể dọn các gói premium tạm", e);
        }
    }
}
