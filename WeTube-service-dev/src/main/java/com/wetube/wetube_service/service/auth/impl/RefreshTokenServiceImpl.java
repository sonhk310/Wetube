package com.wetube.wetube_service.service.auth.impl;

import com.wetube.wetube_service.dto.IssueResult;
import com.wetube.wetube_service.dto.RotateResult;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.auth.RefreshToken;
import com.wetube.wetube_service.exception.InvalidTokenException;
import com.wetube.wetube_service.exception.SystemConfigurationException;
import com.wetube.wetube_service.repository.auth.RefreshTokenRepository;
import com.wetube.wetube_service.service.auth.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-token-ttl-days}")
    private long ttlDays;

    private static String newRaw() {
        return UUID.randomUUID().toString() + "." + UUID.randomUUID();
    }

    private static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new SystemConfigurationException("Error while hashing", e);
        }
    }

    @Override
    @Transactional
    public IssueResult issue(AppUser user, @Nullable String existingSid) {
        // Nếu refresh-login thì existingSid chính là SID từ cookie
        String sid = (existingSid != null && !existingSid.isBlank())
                ? existingSid
                : UUID.randomUUID().toString();

        RefreshToken rt = refreshTokenRepository.findByUserIdAndSessionIdForUpdate(user.getId(), sid)
                .orElseGet(() -> {
                    RefreshToken n = new RefreshToken();
                    n.setSessionId(sid);
                    n.setUser(user);
                    n.setCreatedAt(Instant.now());
                    return n;
                });

        String raw = newRaw();

        rt.setUser(user);
        rt.setSessionId(sid);
        rt.setTokenHash(hash(raw));
        rt.setParentTokenHash(null);
        rt.setRevoked(false);
        rt.setExpiresAt(Instant.now().plus(Duration.ofDays(ttlDays)));
        refreshTokenRepository.saveAndFlush(rt);

        return new IssueResult(sid, raw);
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken validateBySession(String sessionId) {
        log.info("Validating session ID: {}", sessionId);

        var tokenOpt = refreshTokenRepository.findBySessionId(sessionId);

        if (tokenOpt.isEmpty()) {
            log.error("VALIDATION FAILED: SID [{}] does not exist in database.", sessionId);
            throw new InvalidTokenException("invalid_session - not found");
        }

        RefreshToken token = tokenOpt.get();
        log.info("Found token for SID [{}]. Check revoked status...", sessionId);

        if (token.isRevoked()) {
            log.error("VALIDATION FAILED: Token for SID [{}] has been revoked (revoked = true).", sessionId);
            throw new InvalidTokenException("invalid_session - revoked");
        }

        log.info("VALIDATION SUCCESS: Token for SID [{}] valid.", sessionId);
        return token;
    }

    @Override
    @Transactional
    public RotateResult rotateBySession(String sessionId) {
        RefreshToken current = refreshTokenRepository.findBySessionIdAndRevokedFalse(sessionId)
                .orElseThrow(() -> new InvalidTokenException("invalid_session"));

        if (current.getExpiresAt().isBefore(Instant.now())) {
            current.setRevoked(true);
            throw new InvalidTokenException("session_expired");
        }

        current.setRevoked(true);

        String raw = newRaw();
        RefreshToken next = new RefreshToken();
        next.setUser(current.getUser());
        next.setSessionId(sessionId);
        next.setTokenHash(hash(raw));
        next.setParentTokenHash(current.getTokenHash());
        next.setRevoked(false);
        next.setExpiresAt(Instant.now().plus(Duration.ofDays(14)));
        refreshTokenRepository.save(next);

        return new RotateResult(sessionId, raw, next.getUser());
    }

    @Override
    @Transactional
    public void revokeSession(String sessionId) {
        refreshTokenRepository.findBySessionId(sessionId).ifPresent(token -> {
            log.info("Revoking token for SID: {}", sessionId);
            token.setRevoked(true);
        });
    }
}
