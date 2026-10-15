package com.wetube.wetube_service.controller;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.wetube.wetube_service.dto.GoogleUser;
import com.wetube.wetube_service.dto.response.GoogleTokenResponse;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.auth.RefreshToken;
import com.wetube.wetube_service.service.UserService;
import com.wetube.wetube_service.service.auth.GoogleTokenService;
import com.wetube.wetube_service.service.auth.JwtService;
import com.wetube.wetube_service.service.auth.RefreshTokenService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CrossOrigin(value = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private static final String AT_COOKIE = "AT";
    private static final String SID_COOKIE = "SID";
    private static final String SAME_SITE_STRICT = "Strict";
    private static final String SAME_SITE_LAX = "Lax";

    private final GoogleTokenService googleToken;
    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshService;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;
    @Value("${CLIENT_URL}")
    private String clientUrl;

    @GetMapping("/login/google")
    public void redirectToGoogle(HttpServletResponse response) throws IOException {
        String url = UriComponentsBuilder
                .fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .build().toUriString();
        response.sendRedirect(url);
    }

    @GetMapping("/login/google/callback")
    public void googleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (error != null && !error.isEmpty()) {
            log.warn("OAuth2 authentication failed with error: {}", error);
            response.sendRedirect(clientUrl);
            return;
        }

        if (code == null || code.isBlank()) {
            log.error("OAuth2 callback received without a code or an error.");
            response.sendRedirect(clientUrl);
            return;
        }

        GoogleTokenResponse gtr = googleToken.exchangeCode(code);
        GoogleUser googleUser = googleToken.parseAndVerify(gtr.idToken());
        String clientIp = request.getRemoteAddr();
        AppUser user = userService.upsertGoogleUser(googleUser, gtr.scope(), clientIp);

        var issue = refreshService.issue(user, null);
        String access = jwtService.createAccessToken(user.getId(), user.getEmail(), user.getRoleCodes());

        ResponseCookie atCookie = ResponseCookie.from(AT_COOKIE, access)
                .httpOnly(true).secure(false).sameSite(SAME_SITE_STRICT)
                .path("/").maxAge(Duration.ofHours(1)).build();

        ResponseCookie sidCookie = ResponseCookie.from(SID_COOKIE, issue.sessionId())
                .httpOnly(true).secure(false).sameSite(SAME_SITE_STRICT)
                .path("/").maxAge(Duration.ofDays(14)).build();

        // Thêm cookie vào response header
        response.addHeader(HttpHeaders.SET_COOKIE, atCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, sidCookie.toString());

        log.info("LOGIN SUCCESS: Issued new refresh token with SID: {}", issue.sessionId());

        response.sendRedirect(clientUrl);
    }

    @PostMapping("/refresh-login")
    public ResponseEntity<Map<String, Object>> refresh(@CookieValue(name = "SID", required = false) String sid) {
        if (sid == null || sid.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No session"));
        }
        var session = refreshService.validateBySession(sid);
        if (session == null) {
            // clear cookies
            ResponseCookie at0 = ResponseCookie.from("AT", "").maxAge(0).path("/").httpOnly(true).build();
            ResponseCookie sid0 = ResponseCookie.from("SID", "").maxAge(0).path("/").httpOnly(true).build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, at0.toString(), sid0.toString())
                    .body(Map.of("error", "Invalid session"));
        }

        var user = session.getUser();
        refreshService.issue(user, sid);

        String newAT = jwtService.createAccessToken(user.getId(), user.getEmail(), user.getRoleCodes());

        ResponseCookie atCookie = ResponseCookie.from(AT_COOKIE, newAT)
                .httpOnly(true).secure(false)
                .sameSite(SAME_SITE_LAX).path("/")
                .maxAge(Duration.ofHours(1)).build();

        ResponseCookie sidCookie = ResponseCookie.from(SID_COOKIE, sid)
                .httpOnly(true).secure(false)
                .sameSite(SAME_SITE_LAX).path("/")
                .maxAge(Duration.ofDays(14)).build();

        log.info("REFRESH-LOGIN: Received SID from cookie: {}", sid);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, atCookie.toString(), sidCookie.toString())
                .body(Map.of(
                        "refreshed", true,
                        "access_token", newAT,
                        "token_type", "Bearer",
                        "expires_in", 86400));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(value = "SID", required = false) String sid) {
        if (sid != null) {
            try {
                refreshService.revokeSession(sid);
            } catch (Exception e) {
                log.warn("Error while revoking session [{}]: {}", sid, e.getMessage());
            }
        }
        ResponseCookie clearAt = ResponseCookie.from("AT", "").httpOnly(true).secure(true)
                .sameSite(SAME_SITE_STRICT).path("/").maxAge(0).build();
        ResponseCookie clearSid = ResponseCookie.from("SID", "").httpOnly(true).secure(true)
                .sameSite(SAME_SITE_STRICT).path("/").maxAge(0).build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearAt.toString())
                .header(HttpHeaders.SET_COOKIE, clearSid.toString())
                .build();
    }
}
