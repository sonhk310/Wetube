package com.wetube.wetube_service.service.premium.paypal;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wetube.wetube_service.configuration.PaypalConfiguration;

/** Phần gọi HTTP tới PayPal Orders API v2 (tách riêng để test không cần mạng). */
@Component
public class PaypalClient {

    private static final Logger log = LoggerFactory.getLogger(PaypalClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Đơn PayPal vừa tạo: id đơn và link để người mua sang PayPal chấp thuận. */
    public record Order(String id, String approveUrl) {}

    /** Kết quả capture: trạng thái đơn và số tiền đã thu thực tế (value null nếu PayPal không trả). */
    public record Capture(String status, String currency, BigDecimal value) {}

    private final PaypalConfiguration config;
    private final RestTemplate http;

    private String token;
    private Instant tokenExpiresAt = Instant.EPOCH;

    @Autowired
    public PaypalClient(PaypalConfiguration config) {
        this(config, defaultHttp());
    }

    PaypalClient(PaypalConfiguration config, RestTemplate http) {
        this.config = config;
        this.http = http;
    }

    private static RestTemplate defaultHttp() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(20).toMillis());
        return new RestTemplate(factory);
    }

    /** Tạo đơn thu ngay (CAPTURE) với số tiền USD; người mua quay về returnUrl / cancelUrl sau khi chấp thuận / huỷ. */
    public Order createOrder(BigDecimal usd, String description, String returnUrl, String cancelUrl) {
        Map<String, Object> experience = Map.of(
                "return_url", returnUrl,
                "cancel_url", cancelUrl,
                "brand_name", "WeTube",
                "locale", "vi-VN",
                "landing_page", "LOGIN",
                "shipping_preference", "NO_SHIPPING",
                "user_action", "PAY_NOW");
        Map<String, Object> body = Map.of(
                "intent", "CAPTURE",
                "purchase_units", List.of(Map.of(
                        "description", description,
                        "amount", Map.of("currency_code", "USD", "value", usd.toPlainString()))),
                "payment_source", Map.of("paypal", Map.of("experience_context", experience)));
        JsonNode json = call("/v2/checkout/orders", body, null, "tạo đơn");
        String id = json.path("id").asText("");
        String approveUrl = "";
        for (JsonNode link : json.path("links")) {
            String rel = link.path("rel").asText();
            if ("payer-action".equals(rel) || "approve".equals(rel)) {
                approveUrl = link.path("href").asText("");
                break;
            }
        }
        if (id.isBlank() || approveUrl.isBlank()) {
            log.error("PayPal tạo đơn: phản hồi thiếu id hoặc link chấp thuận: {}", json);
            throw new PaypalException("PayPal không trả về liên kết thanh toán.");
        }
        return new Order(id, approveUrl);
    }

    /** Thu tiền của đơn đã được người mua chấp thuận. requestId giữ cho gọi lặp không thu hai lần. */
    public Capture capture(String orderId, String requestId) {
        JsonNode json = call("/v2/checkout/orders/" + orderId + "/capture", Map.of(), requestId, "thu tiền");
        JsonNode capture = json.path("purchase_units").path(0).path("payments").path("captures").path(0);
        String value = capture.path("amount").path("value").asText("");
        // Đơn COMPLETED nhưng khoản thu bị giữ (PENDING) thì không coi là đã thu: lấy trạng thái của khoản thu
        String orderStatus = json.path("status").asText("");
        String captureStatus = capture.path("status").asText("");
        String status = "COMPLETED".equals(orderStatus) && !"COMPLETED".equals(captureStatus) ? captureStatus : orderStatus;
        return new Capture(
                status,
                capture.path("amount").path("currency_code").asText(""),
                value.isBlank() ? null : new BigDecimal(value));
    }

    private JsonNode call(String path, Object body, String requestId, String what) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (requestId != null) {
            headers.set("PayPal-Request-Id", requestId);
        }
        try {
            String raw = http.exchange(config.getBaseUrl() + path, HttpMethod.POST, new HttpEntity<>(body, headers),
                    String.class).getBody();
            return MAPPER.readTree(raw == null ? "{}" : raw);
        } catch (HttpStatusCodeException e) {
            throw failure(what, e);
        } catch (RestClientException | IOException e) {
            log.error("PayPal {}: không gọi được: {}", what, e.getMessage());
            throw new PaypalException("Không kết nối được PayPal, vui lòng thử lại sau.", e);
        }
    }

    private synchronized String accessToken() {
        if (token != null && Instant.now().isBefore(tokenExpiresAt)) {
            return token;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(config.getClientId(), config.getSecret());
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        try {
            String raw = http.exchange(config.getBaseUrl() + "/v1/oauth2/token", HttpMethod.POST,
                    new HttpEntity<>(form, headers), String.class).getBody();
            JsonNode json = MAPPER.readTree(raw == null ? "{}" : raw);
            String value = json.path("access_token").asText("");
            if (value.isBlank()) {
                throw new PaypalException("PayPal không cấp được phiên làm việc.");
            }
            token = value;
            tokenExpiresAt = Instant.now().plusSeconds(Math.max(json.path("expires_in").asLong(300) - 60, 30));
            return token;
        } catch (HttpStatusCodeException e) {
            throw failure("đăng nhập", e);
        } catch (RestClientException | IOException e) {
            log.error("PayPal đăng nhập: không gọi được: {}", e.getMessage());
            throw new PaypalException("Không kết nối được PayPal, vui lòng thử lại sau.", e);
        }
    }

    /** Ghi lại mã + thông báo lỗi của PayPal (không có khoá) để chẩn đoán, trả câu gọn cho người dùng. */
    private PaypalException failure(String what, HttpStatusCodeException e) {
        String name = "";
        String message = "";
        try {
            JsonNode json = MAPPER.readTree(e.getResponseBodyAsString());
            name = json.path("name").asText(json.path("error").asText(""));
            message = json.path("message").asText(json.path("error_description").asText(""));
        } catch (IOException ignored) {
            // phản hồi không phải JSON
        }
        log.error("PayPal {} lỗi HTTP {}: {} {}", what, e.getStatusCode().value(), name, message);
        return new PaypalException("PayPal từ chối yêu cầu " + what + (name.isBlank() ? "" : " (" + name + ")") + ".", e);
    }
}
