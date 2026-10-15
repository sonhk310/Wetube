package com.wetube.wetube_service.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wetube.wetube_service.dto.GoogleUser;
import com.wetube.wetube_service.dto.request.CreatePlaylistRequest;
import com.wetube.wetube_service.entity.auth.OAuthAccount;
import com.wetube.wetube_service.entity.auth.Role;
import com.wetube.wetube_service.repository.auth.OAuthAccountRepository;
import com.wetube.wetube_service.repository.auth.RoleRepository;
import com.wetube.wetube_service.repository.UserRepository;
import com.wetube.wetube_service.dto.response.UserResponseDto;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.exception.ResourceNotFoundException;
import com.wetube.wetube_service.mapper.UserMapper;
import com.wetube.wetube_service.service.PlaylistService;
import com.wetube.wetube_service.service.UserService;
import com.wetube.wetube_service.service.channel.impl.ChannelServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@AllArgsConstructor
@Service
@Transactional
public class UserServiceImpl implements UserService {
    private static final String DEFAULT_ROLE = "ROLE_USER";
    public static final String PROVIDER_GOOGLE = "google";

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RoleRepository roleRepository;
    private final OAuthAccountRepository oauthAccountRepository;
    private final ChannelServiceImpl channelService;
    private final ObjectMapper objectMapper;
    private final PlaylistService playlistService;

    @Override
    public UserResponseDto getUserById(UUID userId) {
        Optional<AppUser> userOptional = userRepository.findById(userId);

        AppUser user = userOptional.orElseThrow(() -> new ResourceNotFoundException("User", "Id", userId.toString()));

        return userMapper.toResponse(user);
    }

    @Transactional
    public AppUser upsertGoogleUser(GoogleUser googleUser, String scopes, String clientIp) {
//        Optional<OAuthAccount> oauthAccountOpt = oauthAccountRepository.findByProviderAndProviderUserId(PROVIDER_GOOGLE, googleUser.sub());
//
//        AppUser user;
//        if (oauthAccountOpt.isPresent()) {
//            user = oauthAccountOpt.get().getUser();
//            // Cập nhật thông tin user cũ nếu cần
//            user.setName(googleUser.name());
//            user.setPicture(googleUser.picture());
//        } else {
//            user = userRepository.findByEmail(googleUser.email())
//                    .orElseGet(() -> createNewGoogleUser(googleUser));
//        }
//
//        ensureHasRole(user);
//
//        if (oauthAccountOpt.isEmpty()) {
//            OAuthAccount newAuthAccount = createOAuthAccount(googleUser, scopes, user);
//            oauthAccountRepository.save(newAuthAccount);
//        } else {
//            OAuthAccount existingOAuthAccount = oauthAccountOpt.get();
//            existingOAuthAccount.setScopes(scopes);
//            try {
//                String rawInfoJson = objectMapper.writeValueAsString(googleUser);
//                existingOAuthAccount.setRawInfo(rawInfoJson);
//            } catch (JsonProcessingException e) {
//                log.error("Failed to serialize GoogleUser to JSON for user: {}", googleUser.email(), e);
//                existingOAuthAccount.setRawInfo("{\"error\":\"Serialization failed\"}");
//            }
//            oauthAccountRepository.save(existingOAuthAccount);
//        }

        AppUser user = findOrCreateUserForGoogleLogin(googleUser);
        if (user.getId() == null) {
            userRepository.saveAndFlush(user);
        }

        ensureHasRole(user);
        upsertOAuthAccount(googleUser, scopes, user);
        AppUser savedUser = userRepository.save(user);

//        if (user.getChannel() == null) {
//            channelService.initiateChannel(user.getId(), clientIp);
//            playlistService.initiatePlaylist(user.getId());
//            AppUser finalUser = user;
//            user = userRepository.findById(user.getId())
//                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", finalUser.getId().toString()));
//        }
        if (savedUser.getChannel() == null) {
            channelService.initiateChannel(savedUser.getId(), clientIp);
            playlistService.initiatePlaylist(savedUser.getId());
        }

//        return userRepository.save(user);
        return savedUser;
    }

    private AppUser findOrCreateUserForGoogleLogin(GoogleUser googleUser) {
        Optional<OAuthAccount> oauthAccountOpt = oauthAccountRepository.findByProviderAndProviderUserId(PROVIDER_GOOGLE, googleUser.sub());

        if (oauthAccountOpt.isPresent()) {
            AppUser existingUser = oauthAccountOpt.get().getUser();
            existingUser.setName(googleUser.name());
            existingUser.setPicture(googleUser.picture());
            return existingUser;
        } else {
            return userRepository.findByEmail(googleUser.email())
                    .map(existingUser -> {
                        existingUser.setName(googleUser.name());
                        existingUser.setPicture(googleUser.picture());
                        return existingUser;
                    })
                    .orElseGet(() -> createNewGoogleUser(googleUser));
        }
    }

    private void upsertOAuthAccount(GoogleUser googleUser, String scopes, AppUser user) {
        OAuthAccount oauthAccount = oauthAccountRepository.findByProviderAndProviderUserId(PROVIDER_GOOGLE, googleUser.sub())
                .orElse(new OAuthAccount());

        oauthAccount.setProvider(PROVIDER_GOOGLE);
        oauthAccount.setProviderUserId(googleUser.sub());
        oauthAccount.setUser(user);
        oauthAccount.setScopes(scopes);
        try {
            String rawInfoJson = objectMapper.writeValueAsString(googleUser);
            oauthAccount.setRawInfo(rawInfoJson);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize GoogleUser to JSON for user: {}", googleUser.email(), e);
            oauthAccount.setRawInfo("{\"error\":\"Serialization failed\"}");
        }
        oauthAccountRepository.save(oauthAccount);
    }

    private AppUser createNewGoogleUser(GoogleUser googleUser) {
//        Role userRole = roleRepository.findByCode(DEFAULT_ROLE)
//                .orElseThrow(() -> new IllegalStateException("ROLE_USER not found. Please seed the database."));
//
//        AppUser newUser = AppUser.builder()
//                .email(googleUser.email())
//                .name(googleUser.name())
//                .picture(googleUser.picture())
//                .build();
//
//        newUser.addRole(userRole);
//        return userRepository.save(newUser);
        return AppUser.builder()
                .email(googleUser.email())
                .name(googleUser.name())
                .picture(googleUser.picture())
                .build();
    }

//    private OAuthAccount createOAuthAccount(GoogleUser googleUser, String scopes, AppUser user) {
//        OAuthAccount newAuthAccount = new OAuthAccount();
//        newAuthAccount.setProvider(PROVIDER_GOOGLE);
//        newAuthAccount.setProviderUserId(googleUser.sub());
//        newAuthAccount.setUser(user);
//        newAuthAccount.setScopes(scopes);
//
//        try {
//            String rawInfoJson = objectMapper.writeValueAsString(googleUser);
//            newAuthAccount.setRawInfo(rawInfoJson);
//        } catch (JsonProcessingException e) {
//            log.error("Failed to serialize GoogleUser to JSON for user: {}", googleUser.email(), e);
//            newAuthAccount.setRawInfo("{\"error\":\"Serialization failed\"}");
//        }
//        return newAuthAccount;
//    }

    private void ensureHasRole(AppUser user) {
        boolean hasRole = user.getUserRoles().stream()
                .anyMatch(ur -> UserServiceImpl.DEFAULT_ROLE.equals(ur.getRole().getCode()));
        if (hasRole)
            return;

        Role role = roleRepository.findByCode(UserServiceImpl.DEFAULT_ROLE)
                .orElseThrow(() -> new IllegalStateException("Missing role code: " + UserServiceImpl.DEFAULT_ROLE));
        user.addRole(role);
    }

    @Override
    @Transactional(readOnly = true)
    public AppUser getById(UUID id) {
        if (id == null)
            throw new IllegalArgumentException("User ID cannot be null");
        return userRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }
}