package com.wetube.wetube_service.service;

import com.wetube.wetube_service.dto.GoogleUser;
import com.wetube.wetube_service.dto.response.UserResponseDto;
import com.wetube.wetube_service.entity.AppUser;

import java.util.UUID;

public interface UserService {
    UserResponseDto getUserById(UUID userId);
    AppUser upsertGoogleUser(GoogleUser googleUser, String scopes, String clientIp);

    AppUser getById(UUID id);
}
