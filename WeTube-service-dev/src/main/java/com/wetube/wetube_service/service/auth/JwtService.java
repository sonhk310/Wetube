package com.wetube.wetube_service.service.auth;

import java.util.Collection;
import java.util.UUID;

public interface JwtService {
    String createAccessToken(UUID userId, String email, Collection<String> roles);
}
