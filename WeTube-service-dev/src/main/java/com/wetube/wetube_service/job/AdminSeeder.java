package com.wetube.wetube_service.job;

import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.auth.Role;
import com.wetube.wetube_service.repository.UserRepository;
import com.wetube.wetube_service.repository.auth.RoleRepository;
import com.wetube.wetube_service.security.AdminAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Cấp ROLE_ADMIN cho các tài khoản đã tồn tại có email nằm trong ADMIN_EMAILS.
 * Tài khoản chưa đăng nhập lần nào sẽ được cấp ngay lúc đăng nhập lần đầu (UserServiceImpl).
 */
@Component
@Order(2)
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);
    private static final String ADMIN_ROLE = "ROLE_ADMIN";

    private final AdminAccess adminAccess;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public AdminSeeder(AdminAccess adminAccess, UserRepository userRepository, RoleRepository roleRepository) {
        this.adminAccess = adminAccess;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        for (String email : adminAccess.emails()) {
            try {
                grant(email);
            } catch (Exception e) {
                log.error("Không thể cấp {} cho email quản trị", ADMIN_ROLE, e);
            }
        }
    }

    private void grant(String email) {
        Optional<AppUser> found = userRepository.findByEmail(email);
        if (found.isEmpty()) {
            log.info("ADMIN_EMAILS: chưa có tài khoản nào dùng email khai báo, sẽ cấp {} khi đăng nhập lần đầu", ADMIN_ROLE);
            return;
        }
        AppUser user = found.get();
        boolean alreadyAdmin = user.getUserRoles().stream()
                .anyMatch(ur -> ADMIN_ROLE.equals(ur.getRole().getCode()));
        if (alreadyAdmin) {
            log.info("ADMIN_EMAILS: tài khoản quản trị đã có {}", ADMIN_ROLE);
            return;
        }
        Optional<Role> adminRole = roleRepository.findByCode(ADMIN_ROLE);
        if (adminRole.isEmpty()) {
            log.warn("ADMIN_EMAILS: chưa có role {} trong database", ADMIN_ROLE);
            return;
        }
        user.addRole(adminRole.get());
        userRepository.save(user);
        log.info("ADMIN_EMAILS: đã cấp {} cho tài khoản quản trị", ADMIN_ROLE);
    }
}
