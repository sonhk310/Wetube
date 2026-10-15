package com.wetube.wetube_service.controller;

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
            @RequestParam UUID userId,
            @RequestParam UUID membershipId) {
        return ResponseEntity.ok(transactionService.purchaseMembership(userId, membershipId));
    }
}
