package com.wetube.wetube_service.service.premium.paypal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.wetube.wetube_service.configuration.PaypalConfiguration;

/** Phần gọi PayPal Orders API v2: đúng địa chỉ, đúng nội dung, đọc đúng phản hồi, lỗi không lộ khoá. */
class PaypalClientTest {

    private static final String BASE = "https://api-m.sandbox.paypal.com";
    private static final String TOKEN_JSON = "{\"access_token\":\"A21AAtoken\",\"expires_in\":32400}";

    private final PaypalConfiguration config = new PaypalConfiguration();
    private final RestTemplate http = new RestTemplate();
    private MockRestServiceServer server;
    private PaypalClient client;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(config, "clientId", "my-client-id");
        ReflectionTestUtils.setField(config, "secret", "my-secret-value");
        ReflectionTestUtils.setField(config, "baseUrl", BASE);
        server = MockRestServiceServer.bindTo(http).build();
        client = new PaypalClient(config, http);
    }

    private void expectToken() {
        String basic = "Basic " + Base64.getEncoder().encodeToString("my-client-id:my-secret-value".getBytes());
        server.expect(requestTo(BASE + "/v1/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", basic))
                .andExpect(content().string("grant_type=client_credentials"))
                .andRespond(withSuccess(TOKEN_JSON, MediaType.APPLICATION_JSON));
    }

    @Test
    void createOrderSendsAUsdCaptureOrderAndReturnsTheApproveLink() {
        expectToken();
        server.expect(requestTo(BASE + "/v2/checkout/orders"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer A21AAtoken"))
                .andExpect(jsonPath("$.intent").value("CAPTURE"))
                .andExpect(jsonPath("$.purchase_units[0].amount.currency_code").value("USD"))
                .andExpect(jsonPath("$.purchase_units[0].amount.value").value("3.83"))
                .andExpect(jsonPath("$.purchase_units[0].description").value("WeTube Premium - Basic Plan"))
                .andExpect(jsonPath("$.payment_source.paypal.experience_context.return_url").value("https://api.test/return"))
                .andExpect(jsonPath("$.payment_source.paypal.experience_context.cancel_url").value("https://api.test/cancel"))
                .andExpect(jsonPath("$.payment_source.paypal.experience_context.user_action").value("PAY_NOW"))
                .andRespond(withSuccess("{\"id\":\"ORDER1\",\"status\":\"PAYER_ACTION_REQUIRED\",\"links\":["
                        + "{\"href\":\"https://api.sandbox.paypal.com/v2/checkout/orders/ORDER1\",\"rel\":\"self\"},"
                        + "{\"href\":\"https://www.sandbox.paypal.com/checkoutnow?token=ORDER1\",\"rel\":\"payer-action\"}]}",
                        MediaType.APPLICATION_JSON));

        PaypalClient.Order order = client.createOrder(new BigDecimal("3.83"), "WeTube Premium - Basic Plan",
                "https://api.test/return", "https://api.test/cancel");

        assertThat(order.id()).isEqualTo("ORDER1");
        assertThat(order.approveUrl()).isEqualTo("https://www.sandbox.paypal.com/checkoutnow?token=ORDER1");
        server.verify();
    }

    @Test
    void theAccessTokenIsReusedAcrossCalls() {
        expectToken();
        for (int i = 0; i < 2; i++) {
            server.expect(requestTo(BASE + "/v2/checkout/orders"))
                    .andRespond(withSuccess("{\"id\":\"O" + i + "\",\"links\":[{\"href\":\"https://p/\",\"rel\":\"approve\"}]}",
                            MediaType.APPLICATION_JSON));
        }

        client.createOrder(BigDecimal.ONE, "d", "r", "c");
        client.createOrder(BigDecimal.ONE, "d", "r", "c");

        server.verify();
    }

    @Test
    void aResponseWithoutAnApproveLinkIsAnError() {
        expectToken();
        server.expect(requestTo(BASE + "/v2/checkout/orders"))
                .andRespond(withSuccess("{\"id\":\"ORDER1\",\"links\":[]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.createOrder(BigDecimal.ONE, "d", "r", "c")).isInstanceOf(PaypalException.class);
    }

    @Test
    void captureReadsTheStatusAndTheAmountActuallyCollected() {
        expectToken();
        server.expect(requestTo(BASE + "/v2/checkout/orders/ORDER1/capture"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("PayPal-Request-Id", "req-1"))
                .andRespond(withSuccess("{\"id\":\"ORDER1\",\"status\":\"COMPLETED\",\"purchase_units\":[{\"payments\":{\"captures\":["
                        + "{\"id\":\"C1\",\"status\":\"COMPLETED\",\"amount\":{\"currency_code\":\"USD\",\"value\":\"3.83\"}}]}}]}",
                        MediaType.APPLICATION_JSON));

        PaypalClient.Capture capture = client.capture("ORDER1", "req-1");

        assertThat(capture.status()).isEqualTo("COMPLETED");
        assertThat(capture.currency()).isEqualTo("USD");
        assertThat(capture.value()).isEqualByComparingTo("3.83");
    }

    @Test
    void anOrderCompletedButWithAHeldCaptureIsNotReportedAsCompleted() {
        expectToken();
        server.expect(requestTo(BASE + "/v2/checkout/orders/ORDER1/capture"))
                .andRespond(withSuccess("{\"status\":\"COMPLETED\",\"purchase_units\":[{\"payments\":{\"captures\":["
                        + "{\"status\":\"PENDING\",\"amount\":{\"currency_code\":\"USD\",\"value\":\"3.83\"}}]}}]}",
                        MediaType.APPLICATION_JSON));

        assertThat(client.capture("ORDER1", "r").status()).isEqualTo("PENDING");
    }

    @Test
    void payPalErrorsBecomeAShortMessageWithTheErrorNameAndNoSecrets() {
        expectToken();
        server.expect(requestTo(BASE + "/v2/checkout/orders/ORDER1/capture"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"name\":\"UNPROCESSABLE_ENTITY\",\"message\":\"business validation\",\"details\":[{\"issue\":\"ORDER_NOT_APPROVED\"}]}"));

        assertThatThrownBy(() -> client.capture("ORDER1", "r")).isInstanceOfSatisfying(PaypalException.class, e -> {
            assertThat(e.getMessage()).contains("UNPROCESSABLE_ENTITY: ORDER_NOT_APPROVED")
                    .doesNotContain("my-secret-value").doesNotContain("A21AAtoken");
        });
    }

    @Test
    void badCredentialsAreReportedWithoutEchoingThem() {
        server.expect(requestTo(BASE + "/v1/oauth2/token"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"invalid_client\",\"error_description\":\"Client Authentication failed\"}"));

        assertThatThrownBy(() -> client.createOrder(BigDecimal.ONE, "d", "r", "c")).isInstanceOfSatisfying(PaypalException.class, e -> {
            assertThat(e.getMessage()).contains("invalid_client").doesNotContain("my-secret-value").doesNotContain("my-client-id");
        });
    }
}
