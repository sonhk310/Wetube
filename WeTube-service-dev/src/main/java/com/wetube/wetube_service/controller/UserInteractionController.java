package com.wetube.wetube_service.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.wetube.wetube_service.security.CurrentUser;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.wetube.wetube_service.dto.request.InteractionRequest;
import com.wetube.wetube_service.service.interaction.impl.UserInteractionServiceImpl;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interactions")
public class UserInteractionController {
    private final UserInteractionServiceImpl interactionService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> saveInteraction(@RequestBody InteractionRequest request, @AuthenticationPrincipal Jwt jwt) {
        request.setUserId(CurrentUser.id(jwt));
        if (request.getVideoId() == null || request.getType() == null) {
            return ResponseEntity.badRequest().body("Missing parameters!");
        }
        interactionService.saveInteraction(request.getUserId(), request.getVideoId(), request.getType());
        return ResponseEntity.ok("Interaction saved successfully.");
    }
}
