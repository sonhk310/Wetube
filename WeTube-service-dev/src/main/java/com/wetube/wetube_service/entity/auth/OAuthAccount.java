package com.wetube.wetube_service.entity.auth;

import com.wetube.wetube_service.entity.AppUser;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Getter
@Setter
@Table(name = "oauth_accounts", uniqueConstraints = @UniqueConstraint(columnNames = { "provider", "provider_user_id" }))
public class OAuthAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false)
    private String provider; // 'google'

    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId; // sub

    private String email;
    private String name;
    private String picture;

    @Column(columnDefinition = "text")
    private String scopes;

    @Column(columnDefinition = "json")
    private String rawInfo;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void upd() {
        updatedAt = Instant.now();
    }
}
