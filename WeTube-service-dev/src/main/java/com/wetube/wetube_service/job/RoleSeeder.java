package com.wetube.wetube_service.job;

import com.wetube.wetube_service.entity.auth.Role;
import com.wetube.wetube_service.repository.auth.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Đảm bảo các role mặc định luôn tồn tại (database mới chưa chạy Init_Struct.sql).
 */
@Component
@Order(0)
public class RoleSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RoleSeeder.class);
    private static final List<String> DEFAULT_ROLES = List.of("ROLE_USER", "ROLE_ADMIN");

    private final RoleRepository roleRepository;

    public RoleSeeder(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        for (String code : DEFAULT_ROLES) {
            try {
                if (roleRepository.findByCode(code).isEmpty()) {
                    Role role = new Role();
                    role.setCode(code);
                    roleRepository.save(role);
                    log.info("Đã tạo role mặc định {}", code);
                }
            } catch (Exception e) {
                log.error("Không thể tạo role mặc định {}", code, e);
            }
        }
    }
}
