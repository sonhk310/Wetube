    package com.wetube.wetube_service.service.premium.impl;

    import java.time.LocalDate;
    import java.util.Base64;
    import java.util.HashMap;
    import java.util.Map;
    import java.util.UUID;

    import org.springframework.http.HttpEntity;
    import org.springframework.http.HttpHeaders;
    import org.springframework.http.HttpMethod;
    import org.springframework.http.MediaType;
    import org.springframework.http.ResponseEntity;
    import org.springframework.stereotype.Service;
    import org.springframework.web.client.RestTemplate;

import com.wetube.wetube_service.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wetube.wetube_service.configuration.MomoConfiguration;
    import com.wetube.wetube_service.dto.request.PaymentRequestDto;
    import com.wetube.wetube_service.dto.response.PaymentResponseDto;
    import com.wetube.wetube_service.entity.AppUser;
    import com.wetube.wetube_service.entity.premium.PaymentMomo;
    import com.wetube.wetube_service.entity.premium.PremiumUser;
    import com.wetube.wetube_service.entity.premium.SubPack;
    import com.wetube.wetube_service.enumeration.ActiveStatus;
    import com.wetube.wetube_service.enumeration.PaymentStatus;
    import com.wetube.wetube_service.repository.premium.PaymentMomoRepository;
    import com.wetube.wetube_service.repository.premium.PremiumUserRepository;
    import com.wetube.wetube_service.repository.premium.SubPackRepository;
    import com.wetube.wetube_service.repository.UserRepository;
    import com.wetube.wetube_service.service.premium.PaymentService;
import com.wetube.wetube_service.utility.HmacSHA256Util;

import jakarta.transaction.Transactional;
    import lombok.RequiredArgsConstructor;

    @Service
    @RequiredArgsConstructor
    public class PaymentServiceImpl implements PaymentService {

        private final MomoConfiguration momoConfiguration;
        private final PaymentMomoRepository paymentRepo;
        private final PremiumUserRepository premiumUserRepo;
        private final SubPackRepository subPackRepo;
        private final UserRepository userRepo;

        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PaymentServiceImpl.class);

        /** Đặt MOMO_VERIFY_SIGNATURE=false để tạm tắt kiểm tra chữ ký nếu cần gỡ lỗi. */
        @org.springframework.beans.factory.annotation.Value("${MOMO_VERIFY_SIGNATURE:true}")
        private boolean verifySignature;

        /**
         * MoMo ký các tham số callback bằng secretKey (HMAC-SHA256). Không có chữ ký hợp lệ thì
         * ai cũng giả được kết quả thanh toán bằng cách tự gọi /redirect hoặc /ipn.
         */
        private boolean isValidMomoSignature(Map<String, String> p) {
            try {
                String signature = p.get("signature");
                if (signature == null || signature.isBlank()) {
                    return false;
                }
                String raw = "accessKey=" + momoConfiguration.getAccessKey()
                        + "&amount=" + nz(p.get("amount"))
                        + "&extraData=" + nz(p.get("extraData"))
                        + "&message=" + nz(p.get("message"))
                        + "&orderId=" + nz(p.get("orderId"))
                        + "&orderInfo=" + nz(p.get("orderInfo"))
                        + "&orderType=" + nz(p.get("orderType"))
                        + "&partnerCode=" + nz(p.get("partnerCode"))
                        + "&payType=" + nz(p.get("payType"))
                        + "&requestId=" + nz(p.get("requestId"))
                        + "&responseTime=" + nz(p.get("responseTime"))
                        + "&resultCode=" + nz(p.get("resultCode"))
                        + "&transId=" + nz(p.get("transId"));
                String expected = HmacSHA256Util.sign(raw, momoConfiguration.getSecretKey());
                return java.security.MessageDigest.isEqual(
                        expected.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        signature.trim().toLowerCase().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            } catch (Exception e) {
                log.error("Không kiểm tra được chữ ký MoMo", e);
                return false;
            }
        }

        private static String nz(String v) {
            return v == null ? "" : v;
        }

        @Override
        public PaymentResponseDto createPayment(PaymentRequestDto dto) {
            try {
                    String orderId = UUID.randomUUID().toString();
                    String requestId = UUID.randomUUID().toString();

                    SubPack subPack = subPackRepo.findById(dto.getSubPackId())
                            .orElseThrow(() -> new ResourceNotFoundException("SubPack not found"));
                    AppUser user = userRepo.findById(dto.getUserId())
                            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                    Long amountLong = subPack.getPrice().longValue(); 
                    String amountStr = amountLong.toString();

                    String extraDataJson = "{\"skus\":\"\"}";
                    String extraDataEncoded = Base64.getEncoder().encodeToString(extraDataJson.getBytes());
                    String orderInfo = "Thank you for your purchase at MoMo_test";
                   
                    String rawData = "accessKey=" + momoConfiguration.getAccessKey() +
                        "&amount=" + amountStr +
                        "&extraData=" + extraDataEncoded +
                        "&ipnUrl=" + momoConfiguration.getIpnUrl() +
                        "&orderId=" + orderId +
                        "&orderInfo=" + orderInfo +
                        "&partnerCode=" + momoConfiguration.getPartnerCode() +
                        "&redirectUrl=" + momoConfiguration.getRedirectUrl() +
                        "&requestId=" + requestId +
                        "&requestType=captureWallet";

                    String signature = HmacSHA256Util.sign(rawData, momoConfiguration.getSecretKey());

                    Map<String, Object> body = new HashMap<>();
                    body.put("partnerCode", momoConfiguration.getPartnerCode());
                    body.put("accessKey", momoConfiguration.getAccessKey());
                    body.put("requestId", requestId);
                    body.put("amount", amountStr);
                    body.put("orderId", orderId);
                    body.put("orderInfo", "Thank you for your purchase at MoMo_test");
                    body.put("redirectUrl", momoConfiguration.getRedirectUrl());
                    body.put("ipnUrl", momoConfiguration.getIpnUrl());
                    body.put("extraData", extraDataEncoded);
                    body.put("requestType", "captureWallet");
                    body.put("signature", signature);
                    body.put("lang", "vi");

                    RestTemplate restTemplate = new RestTemplate();
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);

                    HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
                    ResponseEntity<Map> response = restTemplate.exchange(
                            momoConfiguration.getEndpoint(),
                            HttpMethod.POST,
                            requestEntity,
                            Map.class
                    );
                    String payUrl = (String) response.getBody().get("payUrl");

                    Map<String, Object> momoResp = response.getBody();
                    
                    // Tạo PaymentMomo PENDING trước, chưa tạo PremiumUser
                    PaymentMomo payment = PaymentMomo.builder()
                            .orderId(orderId)
                            .requestId(requestId)
                            .amount(amountLong)
                            .provider("MOMO")
                            .status(PaymentStatus.PENDING) 
                            .subpack(subPack)
                            .user(user)
                            .payload("{}") 
                            .build();

                    paymentRepo.save(payment);

                    return PaymentResponseDto.builder()
                            .payUrl((String) momoResp.get("payUrl"))
                            .deeplink((String) momoResp.get("deeplink"))
                            .qrCodeUrl((String) momoResp.get("qrCodeUrl"))
                            .orderId(orderId)
                            .requestId(requestId)
                            .amount(amountLong)
                            .message((String) momoResp.get("message"))
                            .resultCode((Integer) momoResp.get("resultCode"))
                            .build();

                } catch (Exception e) {
                    throw new RuntimeException("MoMo Payment failed: " + e.getMessage(), e);
                }
            }

        @Override
        @Transactional
        public String handleMoMoIpn(Map<String, String> params) {
            try {
                String orderId = params.get("orderId");
                String resultCode = params.get("resultCode");

                if (verifySignature && !isValidMomoSignature(params)) {
                    log.warn("Bỏ qua callback MoMo có chữ ký không hợp lệ, orderId={}", orderId);
                    return "{ \"resultCode\": 1, \"message\": \"Invalid signature\" }";
                }

                PaymentMomo payment = paymentRepo.findByOrderId(orderId)
                        .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

                // /redirect và /ipn cùng gọi vào đây: đã xử lý thành công thì không cộng gói lần nữa
                if (payment.getStatus() == PaymentStatus.SUCCESS) {
                    return "{ \"resultCode\": 0, \"message\": \"Confirm Success\" }";
                }

                if ("0".equals(resultCode)) {
                
                    PremiumUser premiumUser = PremiumUser.builder()
                            .user(payment.getUser())
                            .subpack(payment.getSubpack())
                            .startDate(LocalDate.now())
                            .endDate(LocalDate.now().plusDays(payment.getSubpack().getDurationDays()))
                            .status(ActiveStatus.ACTIVE)
                            .build();

                    premiumUserRepo.saveAndFlush(premiumUser);
                 
                    payment.setPremiumUser(premiumUser);
                    payment.setStatus(PaymentStatus.SUCCESS);
                    payment.setEventType("PAYMENT_SUCCESS");

                    
                    paymentRepo.save(payment);
                } else {
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setEventType("PAYMENT_FAILED");
                }


                ObjectMapper mapper = new ObjectMapper();
                String payloadJson = mapper.writeValueAsString(params);
                payment.setPayload(payloadJson);
                paymentRepo.save(payment);

                return "{ \"resultCode\": 0, \"message\": \"Confirm Success\" }";
            } catch (Exception e) {
                return "{ \"resultCode\": 1, \"message\": \"Confirm Failed\" }";
            }
        }

    }
