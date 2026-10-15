package com.wetube.wetube_service.service.premium;

import java.util.Map;

import com.wetube.wetube_service.dto.request.PaymentRequestDto;
import com.wetube.wetube_service.dto.response.PaymentResponseDto;

public interface PaymentService {
    PaymentResponseDto createPayment(PaymentRequestDto dto);
    String handleMoMoIpn(Map<String, String> params);
}
