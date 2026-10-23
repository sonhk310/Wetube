package com.wetube.wetube_service.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Danh sách email được tự động cấp ROLE_ADMIN, cấu hình bằng biến môi trường ADMIN_EMAILS
 * (nhiều email cách nhau bằng dấu phẩy).
 */
@Component
public class AdminAccess {

    private final Set<String> adminEmails;

    public AdminAccess(@Value("${ADMIN_EMAILS:}") String raw) {
        this.adminEmails = Arrays.stream(raw.split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isAdminEmail(String email) {
        return email != null && adminEmails.contains(email.trim().toLowerCase(Locale.ROOT));
    }

    public Set<String> emails() {
        return adminEmails;
    }
}
