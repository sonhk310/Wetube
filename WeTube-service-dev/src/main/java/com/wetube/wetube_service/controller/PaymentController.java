package com.wetube.wetube_service.controller;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

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

    @PostMapping("/create")
    public ResponseEntity<PaymentResponseDto> createPayment(@RequestBody PaymentRequestDto dto) {
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
            String resultCode = queryParams.get("resultCode");
            String orderId = queryParams.get("orderId");

            if ("0".equals(resultCode)) {
                paymentService.handleMoMoIpn(
                    queryParams.entrySet().stream()
                        .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue
                        ))
                );
            }

//            String redirectUrl = "http://localhost:3000/?paymentStatus=" + resultCode + "&orderId=" + orderId;
         String redirectUrl = "https://wetube.name.vn/?paymentStatus=" + resultCode + "&orderId=" + orderId;
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
