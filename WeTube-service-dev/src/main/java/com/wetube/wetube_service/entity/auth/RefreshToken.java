package com.wetube.wetube_service.entity.auth;

import com.wetube.wetube_service.entity.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "uq_refresh_session", columnList = "session_id", unique = true)
})
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // CHAR(36)
    private AppUser user;

    @Column(name = "session_id", nullable = false, unique = true, length = 36)
    private String sessionId; // UUID cho session (opaque ID)

    @Column(name = "token_hash", nullable = false, unique = true, length = 88)
    private String tokenHash; // SHA-256 hash (Base64)

    private Instant expiresAt;
    private boolean revoked;
    private String parentTokenHash; // support rotation chain (optional)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
