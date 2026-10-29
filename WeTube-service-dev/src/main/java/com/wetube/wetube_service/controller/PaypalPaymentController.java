package com.wetube.wetube_service.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wetube.wetube_service.security.CurrentUser;
import com.wetube.wetube_service.service.premium.paypal.PaypalPaymentService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/** Mua Premium bằng PayPal (xem PaypalPaymentService). /return và /cancel do trình duyệt được PayPal chuyển về. */
@RestController
@RequestMapping("/api/payment/paypal")
@RequiredArgsConstructor
public class PaypalPaymentController {

    public record CreateRequest(UUID subPackId) {}

    private final PaypalPaymentService paypalPaymentService;

    @Value("${CLIENT_URL}")
    private String clientUrl;

    /** Người mua lấy từ JWT, không từ body. Trả {approveUrl}: client chuyển trang sang đó. */
    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> create(@RequestBody CreateRequest body, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = CurrentUser.id(jwt);
        return ResponseEntity.ok(Map.of("approveUrl", paypalPaymentService.createOrder(userId, body.subPackId())));
    }

    @GetMapping("/return")
    public void paymentReturn(@RequestParam(value = "token", required = false) String token, HttpServletResponse response)
            throws IOException {
        boolean paid = paypalPaymentService.completeReturn(token);
        redirectToClient(response, paid, token);
    }

    @GetMapping("/cancel")
    public void cancel(@RequestParam(value = "token", required = false) String token, HttpServletResponse response)
            throws IOException {
        paypalPaymentService.cancel(token);
        redirectToClient(response, false, token);
    }

    private void redirectToClient(HttpServletResponse response, boolean paid, String orderId) throws IOException {
        response.sendRedirect(clientUrl.replaceAll("/+$", "") + "/otherservices/payment-result?paymentStatus="
                + (paid ? "0" : "1") + "&orderId=" + (orderId == null ? "" : URLEncoder.encode(orderId, StandardCharsets.UTF_8)));
    }
}
