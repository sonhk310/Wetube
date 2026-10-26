package com.wetube.wetube_service.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.wetube.wetube_service.security.CurrentUser;
import com.wetube.wetube_service.dto.MembershipPurchaseResponse;
import com.wetube.wetube_service.service.membership.TransactionService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * Tắt tạm: API này ghi giao dịch và cộng doanh thu cho kênh mà không có bước thanh toán nào, nên ai đăng
     * nhập cũng "mua" miễn phí được. Mở lại cùng việc hội viên kênh (M1), khi chỉ được gọi sau khi MoMo báo thành công.
     */
    @PostMapping("/purchase")
    public ResponseEntity<MembershipPurchaseResponse> purchaseMembership(
            @RequestParam UUID membershipId,
            @AuthenticationPrincipal Jwt jwt) {
        CurrentUser.id(jwt);
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Tính năng hội viên kênh chưa mở");
    }
}
