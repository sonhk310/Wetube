package com.wetube.wetube_service.job;

import com.wetube.wetube_service.security.AdminAccess;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cấp ROLE_ADMIN cho các tài khoản đã tồn tại có email nằm trong ADMIN_EMAILS.
 * Tài khoản chưa đăng nhập lần nào sẽ được cấp ngay lúc đăng nhập lần đầu (UserServiceImpl).
 */
@Component
@Order(2)
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final AdminAccess adminAccess;

    @PersistenceContext
    private EntityManager em;

    public AdminSeeder(AdminAccess adminAccess) {
        this.adminAccess = adminAccess;
    }

    @Override
    @Transactional
    public void run(String... args) {
        for (String email : adminAccess.emails()) {
            try {
                int granted = em.createNativeQuery(
                                "INSERT INTO users_roles (user_id, role_id) "
                                        + "SELECT u.id, r.id FROM users u JOIN roles r ON r.code = 'ROLE_ADMIN' "
                                        + "WHERE LOWER(u.email) = ?1 "
                                        + "AND NOT EXISTS (SELECT 1 FROM users_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id)")
                        .setParameter(1, email)
                        .executeUpdate();
                if (granted > 0) {
                    log.info("Đã cấp ROLE_ADMIN cho tài khoản quản trị");
                }
            } catch (Exception e) {
                log.error("Không thể cấp ROLE_ADMIN cho email quản trị", e);
            }
        }
    }
}
