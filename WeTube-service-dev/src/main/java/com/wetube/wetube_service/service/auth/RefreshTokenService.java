package com.wetube.wetube_service.service.auth;

import com.wetube.wetube_service.dto.IssueResult;
import com.wetube.wetube_service.dto.RotateResult;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.auth.RefreshToken;
import org.springframework.lang.Nullable;

public interface RefreshTokenService {
    IssueResult issue(AppUser user, @Nullable String parentHash);

    RefreshToken validateBySession(String sessionId);

    RotateResult rotateBySession(String sessionId);

    void revokeSession(String sessionId);
}
