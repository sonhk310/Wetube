package com.wetube.wetube_service.service.premium.paypal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wetube.wetube_service.configuration.PaypalConfiguration;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.premium.PaymentMomo;
import com.wetube.wetube_service.entity.premium.SubPack;
import com.wetube.wetube_service.enumeration.PaymentStatus;
import com.wetube.wetube_service.repository.UserRepository;
import com.wetube.wetube_service.repository.premium.PaymentMomoRepository;
import com.wetube.wetube_service.repository.premium.SubPackRepository;
import com.wetube.wetube_service.security.RateLimiter;
import com.wetube.wetube_service.service.premium.PremiumGranter;

import lombok.RequiredArgsConstructor;

/**
 * Mua Premium bằng PayPal: tạo đơn (USD), người mua chấp thuận ở PayPal, quay về thì backend thu tiền (capture) rồi
 * mới cấp gói. Đơn lưu chung bảng payment_momo với provider = PAYPAL, orderId = id đơn của PayPal.
 */
@Service
@RequiredArgsConstructor
public class PaypalPaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaypalPaymentService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    static final String PROVIDER = "PAYPAL";

    private final PaypalConfiguration config;
    private final PaypalClient client;
    private final PaymentMomoRepository paymentRepo;
    private final SubPackRepository subPackRepo;
    private final UserRepository userRepo;
    private final PremiumGranter premiumGranter;
    private final RateLimiter rateLimiter;

    /** VND -> USD theo tỉ giá cố định, làm tròn 2 chữ số, tối thiểu 0,01. */
    static BigDecimal toUsd(BigDecimal vnd, BigDecimal vndPerUsd) {
        return vnd.divide(vndPerUsd, 2, RoundingMode.HALF_UP).max(new BigDecimal("0.01"));
    }

    /** Tạo đơn PayPal cho gói, trả link để người mua sang PayPal chấp thuận. 503 chưa cấu hình, 502 PayPal lỗi. */
    public String createOrder(UUID userId, UUID subPackId) {
        if (!config.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Thanh toán PayPal chưa được cấu hình.");
        }
        rateLimiter.check(RateLimiter.Limit.PAYMENT_CREATE, userId);
        AppUser user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
        SubPack pack = subPackRepo.findById(subPackId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy gói Premium"));

        BigDecimal usd = toUsd(pack.getPrice(), config.getVndPerUsd());
        String base = config.getReturnBase().replaceAll("/+$", "");
        PaypalClient.Order order;
        try {
            order = client.createOrder(usd, "WeTube Premium - " + pack.getName(), base + "/return", base + "/cancel");
        } catch (PaypalException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage());
        }

        var payload = MAPPER.createObjectNode().put("usd", usd.toPlainString()).put("vndPerUsd", config.getVndPerUsd().toPlainString());
        paymentRepo.save(PaymentMomo.builder()
                .orderId(order.id())
                .provider(PROVIDER)
                .amount(pack.getPrice().longValue())
                .status(PaymentStatus.PENDING)
                .subpack(pack)
                .user(user)
                .payload(payload.toString())
                .build());
        return order.approveUrl();
    }

    /**
     * Người mua quay về từ PayPal (token = id đơn). Thu tiền rồi cấp gói; chỉ cấp khi PayPal báo COMPLETED đúng số
     * tiền USD đã lưu. Gọi lại cho đơn đã cấp thì trả true và không cấp thêm. PayPal lỗi tạm thời thì đơn vẫn
     * PENDING (gọi lại được). Trả true khi đơn đã thành công.
     */
    @Transactional
    public boolean completeReturn(String token) {
        Optional<PaymentMomo> found = token == null ? Optional.empty()
                : paymentRepo.findByOrderIdForUpdate(token).filter(p -> PROVIDER.equals(p.getProvider()));
        if (found.isEmpty()) {
            log.warn("PayPal /return với đơn không có trong hệ thống");
            return false;
        }
        PaymentMomo payment = found.get();
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return true;
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return false;
        }

        PaypalClient.Capture capture;
        try {
            capture = client.capture(token, payment.getId() != null ? payment.getId().toString() : token);
        } catch (PaypalException e) {
            log.warn("PayPal thu tiền đơn {} chưa được: {}", token, e.getMessage());
            return false;
        }

        BigDecimal expected = expectedUsd(payment);
        boolean paid = "COMPLETED".equals(capture.status()) && "USD".equals(capture.currency())
                && capture.value() != null && expected != null && capture.value().compareTo(expected) == 0;
        if (!paid) {
            log.error("PayPal đơn {} không hợp lệ để cấp gói: trạng thái={}, thu {} {}, cần {} USD", token,
                    capture.status(), capture.value(), capture.currency(), expected);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setEventType("PAYMENT_FAILED");
            paymentRepo.save(payment);
            return false;
        }
        premiumGranter.grant(payment);
        paymentRepo.save(payment);
        return true;
    }

    /** Người mua bấm huỷ ở PayPal: đơn đang chờ thì thành FAILED. */
    @Transactional
    public void cancel(String token) {
        if (token == null) {
            return;
        }
        paymentRepo.findByOrderIdForUpdate(token)
                .filter(p -> PROVIDER.equals(p.getProvider()) && p.getStatus() == PaymentStatus.PENDING)
                .ifPresent(p -> {
                    p.setStatus(PaymentStatus.FAILED);
                    p.setEventType("PAYMENT_CANCELED");
                    paymentRepo.save(p);
                });
    }

    private static BigDecimal expectedUsd(PaymentMomo payment) {
        try {
            JsonNode usd = MAPPER.readTree(payment.getPayload()).path("usd");
            return usd.isMissingNode() ? null : new BigDecimal(usd.asText());
        } catch (Exception e) {
            return null;
        }
    }
}
