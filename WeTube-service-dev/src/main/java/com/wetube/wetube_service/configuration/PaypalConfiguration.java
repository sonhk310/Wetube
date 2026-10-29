package com.wetube.wetube_service.configuration;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

/**
 * Thanh toán PayPal (sandbox hoặc thật tuỳ PAYPAL_BASE). Thiếu PAYPAL_CLIENT_ID / PAYPAL_SECRET / PAYPAL_RETURN_BASE
 * thì PayPal coi như chưa cấu hình (nút báo lỗi rõ ràng), MoMo không bị ảnh hưởng. Khoá chỉ đến từ biến môi trường.
 */
@Configuration
@Getter
public class PaypalConfiguration {

    @Value("${PAYPAL_CLIENT_ID:}")
    private String clientId;

    @Value("${PAYPAL_SECRET:}")
    private String secret;

    @Value("${PAYPAL_BASE:https://api-m.sandbox.paypal.com}")
    private String baseUrl;

    /** Địa chỉ công khai của backend cho /return và /cancel, vd. https://wetube-service.vibe.tinhgon.xyz/api/payment/paypal */
    @Value("${PAYPAL_RETURN_BASE:}")
    private String returnBase;

    /** PayPal không có VND: đổi sang USD theo tỉ giá cố định này (số VND cho 1 USD). */
    @Value("${PAYPAL_VND_PER_USD:25835.5}")
    private BigDecimal vndPerUsd;

    public boolean isConfigured() {
        return !clientId.isBlank() && !secret.isBlank() && !returnBase.isBlank();
    }
}
