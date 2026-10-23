package com.wetube.wetube_service.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.wetube.wetube_service.security.CurrentUser;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wetube.wetube_service.dto.request.PaymentRequestDto;
import com.wetube.wetube_service.dto.response.PaymentResponseDto;
import com.wetube.wetube_service.repository.premium.PaymentMomoRepository;
import com.wetube.wetube_service.service.premium.PaymentService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payment/momo")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    private final PaymentMomoRepository paymentRepo;

    @Value("${CLIENT_URL}")
    private String clientUrl;

    @PostMapping("/create")
    public ResponseEntity<PaymentResponseDto> createPayment(@RequestBody PaymentRequestDto dto, @AuthenticationPrincipal Jwt jwt) {
        dto.setUserId(CurrentUser.id(jwt));
        return ResponseEntity.ok(paymentService.createPayment(dto));
    }

    @PostMapping("/ipn")
    public ResponseEntity<String> handleIpn(@RequestBody Map<String, Object> params) {
        // cast các giá trị sang String
        Map<String, String> stringParams = params.entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> String.valueOf(e.getValue())
            ));

        String response = paymentService.handleMoMoIpn(stringParams);
        return ResponseEntity.ok(response);
    }

     @GetMapping("/redirect")
        public void handleRedirect(
                @RequestParam Map<String, String> queryParams,
                HttpServletResponse response) throws IOException {  
            String orderId = queryParams.get("orderId");

            // Ghi nhận kết quả (có kiểm tra chữ ký MoMo), rồi hiển thị theo trạng thái thật trong DB
            // chứ không tin tham số resultCode trên URL.
            paymentService.handleMoMoIpn(
                queryParams.entrySet().stream()
                    .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                    ))
            );
            String resultCode = orderId == null ? "1"
                    : paymentRepo.findByOrderId(orderId)
                        .map(p -> p.getStatus() == com.wetube.wetube_service.enumeration.PaymentStatus.SUCCESS ? "0" : "1")
                        .orElse("1");

//            String redirectUrl = "http://localhost:3000/?paymentStatus=" + resultCode + "&orderId=" + orderId;
            String redirectUrl = clientUrl.replaceAll("/+$", "") + "/otherservices/payment-result?paymentStatus=" + resultCode + "&orderId=" + orderId;
            response.sendRedirect(redirectUrl); 
        }

    @PostMapping("/notify")
    public ResponseEntity<String> handleNotify(@RequestBody Map<String, Object> body) {
        System.out.println("MoMo notify: " + body);
        return ResponseEntity.ok("OK");
    }

    // @PostMapping("/confirm")
    // public ResponseEntity<String> confirmPayment(@RequestParam String orderId) {
    //     PaymentMomo payment = paymentRepo.findByOrderId(orderId)
    //         .orElseThrow(() -> new RuntimeException("Payment not found"));

    //     if (payment.getStatus() == PaymentStatus.SUCCESS) {
    //         return ResponseEntity.ok("Order đã được xác nhận thành công");
    //     }
    //     return ResponseEntity.badRequest().body("Thanh toán chưa thành công hoặc đã thất bại");
    // }

}
