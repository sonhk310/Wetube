package com.wetube.wetube_service.utility;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

public final class CookieUtility {
    private CookieUtility() {
    }

    public static void set(HttpServletResponse res, String name, String value, int maxAgeSec,
            String domain, boolean secure, String sameSite, String path) {
        Cookie c = new Cookie(name, value);
        c.setHttpOnly(true);
        c.setSecure(secure);
        c.setMaxAge(maxAgeSec);
        c.setPath(path);
        if (domain != null && !domain.isBlank())
            c.setDomain(domain);
        res.addHeader("Set-Cookie",
                "%s=%s; Max-Age=%d; Path=%s; %s%s%s".formatted(
                        name, value, maxAgeSec, path,
                        secure ? "Secure; " : "",
                        "HttpOnly; ",
                        sameSite != null ? "SameSite=" + sameSite : ""));
    }
}
