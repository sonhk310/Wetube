package com.wetube.wetube_service.service.auth.impl;


import com.wetube.wetube_service.service.auth.CookieService;
import com.wetube.wetube_service.utility.CookieUtility;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CookieServiceImpl implements CookieService {

    @Value("${app.cookie.domain:localhost}")
    private String domain;
    @Value("${app.jwt.access-minutes:15}")
    private int accessMinutes;
    @Value("${app.jwt.refresh-days:14}")
    private int refreshDays;

    @Override
    public void setAccessCookie(HttpServletResponse res, String token) {
        CookieUtility.set(res, "access-token", token, accessMinutes * 60, domain, true, "Lax", "/");
    }

    @Override
    public void setRefreshCookie(HttpServletResponse res, String token) {
        CookieUtility.set(res, "REFRESH_TOKEN", token, refreshDays * 24 * 3600, domain, true, "Strict", "/auth");
    }
}
