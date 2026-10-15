package com.wetube.wetube_service.controller;

import com.wetube.wetube_service.dto.PremiumUserDto;
import com.wetube.wetube_service.dto.response.UserResponseDto;
import com.wetube.wetube_service.service.premium.PremiumUserService;
import com.wetube.wetube_service.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@AllArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final PremiumUserService premiumUserService;

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/premium")
    public ResponseEntity<List<PremiumUserDto>> getAllPremiumUsers() {
        List<PremiumUserDto> list = premiumUserService.getAllPremiumUsers();
        return ResponseEntity.ok(list);
    }


//    @GetMapping("/me")
//    public ResponseEntity<UserResponseDto> getCurrentUser(Authentication authentication) {
//        // Principal bây giờ là một đối tượng Jwt
//        Jwt principal = (Jwt) authentication.getPrincipal();
//
//        UserResponseDto currentAppUser = userService.findOrCreateUser(principal);
//
//        return ResponseEntity.ok(currentAppUser);
//    }
}
