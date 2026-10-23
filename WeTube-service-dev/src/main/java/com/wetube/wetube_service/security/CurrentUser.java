package com.wetube.wetube_service.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Lấy danh tính người gọi từ JWT. Không bao giờ tin userId do client gửi trong body hay query.
 */
public final class CurrentUser {

    private static final String ADMIN_ROLE = "ROLE_ADMIN";

    private CurrentUser() {
    }

    /** Id người dùng đang đăng nhập; 401 nếu chưa đăng nhập. */
    public static UUID id(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập");
        }
        return UUID.fromString(jwt.getSubject());
    }

    public static boolean isAdmin(Jwt jwt) {
        if (jwt == null) {
            return false;
        }
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains(ADMIN_ROLE);
    }

    /** 401 nếu chưa đăng nhập, 403 nếu không phải admin. */
    public static void requireAdmin(Jwt jwt) {
        id(jwt);
        if (!isAdmin(jwt)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ quản trị viên mới được thực hiện thao tác này");
        }
    }
}
