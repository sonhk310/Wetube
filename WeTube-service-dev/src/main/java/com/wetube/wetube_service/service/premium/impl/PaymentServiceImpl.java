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

        @Override
        public PaymentResponseDto createPayment(PaymentRequestDto dto) {
            try {
                    String orderId = UUID.randomUUID().toString();
                    String requestId = UUID.randomUUID().toString();

                    SubPack subPack = subPackRepo.findById(dto.getSubPackId())
                            .orElseThrow(() -> new RuntimeException("SubPack not found"));
                    AppUser user = userRepo.findById(dto.getUserId())
                            .orElseThrow(() -> new RuntimeException("User not found"));

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

                PaymentMomo payment = paymentRepo.findByOrderId(orderId)
                        .orElseThrow(() -> new RuntimeException("Payment not found"));

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
