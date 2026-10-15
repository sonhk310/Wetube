package com.wetube.wetube_service.service.auth;

import jakarta.servlet.http.HttpServletResponse;

public interface CookieService {
    void setAccessCookie(HttpServletResponse res, String token);
    void setRefreshCookie(HttpServletResponse res, String token);
}
