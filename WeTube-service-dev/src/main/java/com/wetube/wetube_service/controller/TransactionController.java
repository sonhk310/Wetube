package com.wetube.wetube_service.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.wetube.wetube_service.security.CurrentUser;
import com.wetube.wetube_service.dto.MembershipPurchaseResponse;
import com.wetube.wetube_service.service.membership.TransactionService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/purchase")
    public ResponseEntity<MembershipPurchaseResponse> purchaseMembership(
            @RequestParam UUID membershipId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(transactionService.purchaseMembership(CurrentUser.id(jwt), membershipId));
    }
}
