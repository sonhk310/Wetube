package com.wetube.wetube_service.controller;

import com.wetube.wetube_service.dto.response.MeResponseDto;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.mapper.MeMapper;
import com.wetube.wetube_service.service.UserService;
import com.wetube.wetube_service.service.auth.RefreshTokenService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/me")
@AllArgsConstructor
public class MeController {
    private static final String ERROR = "error";
    private static final String ERROR_CODE = "error_code";
    private static final String MESSAGE = "message";

    private final RefreshTokenService refreshTokenService;
    private final UserService userService;
    private final MeMapper meMapper;


    @GetMapping
    public ResponseEntity<Object> me(
            @AuthenticationPrincipal Jwt jwt,
            @CookieValue(name = "SID", required = false) String sid) {
        if (jwt == null) {
            if (sid != null && !sid.isBlank()) {
                try {
                    var session = refreshTokenService.validateBySession(sid);
                    if (session.isRevoked()) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                                ERROR, "Already logged out",
                                ERROR_CODE, "session_revoked",
                                MESSAGE, "Session has been revoked"
                        ));
                    }
                    // Nếu session hợp lệ, gợi ý làm mới token
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                            ERROR, "Request new token",
                            ERROR_CODE, "token_required",
                            MESSAGE, "Valid session found, please refresh token"
                    ));
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                            ERROR, "Invalid session",
                            ERROR_CODE, "invalid_session",
                            MESSAGE, e.getMessage() != null ? e.getMessage() : "Invalid session"
                    ));
                }
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    ERROR, "No login yet",
                    ERROR_CODE, "unauthenticated",
                    MESSAGE, "No valid JWT or session provided"
            ));
        }

        String userIdString = jwt.getSubject();
        UUID userId = UUID.fromString(userIdString);
        AppUser user = userService.getById(userId);


        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    ERROR, "User not found",
                    MESSAGE, "User corresponding to JWT not found in database"
            ));
        }

        MeResponseDto response = meMapper.toDto(user);
        return ResponseEntity.ok(response);
    }
}
