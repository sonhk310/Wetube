package com.wetube.wetube_service.service.premium.paypal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.wetube.wetube_service.configuration.PaypalConfiguration;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.premium.PaymentMomo;
import com.wetube.wetube_service.entity.premium.PremiumUser;
import com.wetube.wetube_service.entity.premium.SubPack;
import com.wetube.wetube_service.enumeration.PaymentStatus;
import com.wetube.wetube_service.repository.UserRepository;
import com.wetube.wetube_service.repository.channel.MemberTierRepository;
import com.wetube.wetube_service.repository.premium.PaymentMomoRepository;
import com.wetube.wetube_service.repository.premium.PremiumUserRepository;
import com.wetube.wetube_service.repository.premium.SubPackRepository;
import com.wetube.wetube_service.security.RateLimiter;
import com.wetube.wetube_service.service.membership.ChannelMembershipService;
import com.wetube.wetube_service.service.premium.PremiumGranter;
import com.wetube.wetube_service.service.premium.PremiumUserService;

/** Mua Premium bằng PayPal: đổi USD, tạo đơn, thu tiền khi quay về và chỉ cấp gói một lần khi tiền đúng. */
class PaypalPaymentServiceTest {

    private static final String ORDER_ID = "5O190127TN364715T";

    private final PaypalConfiguration config = new PaypalConfiguration();
    private final PaypalClient client = mock(PaypalClient.class);
    private final PaymentMomoRepository paymentRepo = mock(PaymentMomoRepository.class);
    private final SubPackRepository subPackRepo = mock(SubPackRepository.class);
    private final UserRepository userRepo = mock(UserRepository.class);
    private final PremiumUserRepository premiumRepo = mock(PremiumUserRepository.class);
    private final PremiumUserService premiumService = mock(PremiumUserService.class);
    private final RateLimiter rateLimiter = mock(RateLimiter.class);

    private PaypalPaymentService service;
    private final UUID userId = UUID.randomUUID();
    private final UUID packId = UUID.randomUUID();
    private AppUser user;
    private SubPack pack;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(config, "clientId", "id");
        ReflectionTestUtils.setField(config, "secret", "secret");
        ReflectionTestUtils.setField(config, "baseUrl", "https://api-m.sandbox.paypal.com");
        ReflectionTestUtils.setField(config, "returnBase", "https://api.example.test/api/payment/paypal/");
        ReflectionTestUtils.setField(config, "vndPerUsd", new BigDecimal("25835.5"));
        service = new PaypalPaymentService(config, client, paymentRepo, subPackRepo, userRepo,
                new PremiumGranter(premiumRepo, premiumService), mock(ChannelMembershipService.class),
                mock(MemberTierRepository.class), rateLimiter);

        user = AppUser.builder().id(userId).email("a@b.c").build();
        pack = SubPack.builder().id(packId).name("Basic Plan").price(BigDecimal.valueOf(99_000)).durationDays(30).build();
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(subPackRepo.findById(packId)).thenReturn(Optional.of(pack));
    }

    private PaymentMomo pending(String usd) {
        return PaymentMomo.builder().id(UUID.randomUUID()).orderId(ORDER_ID).provider("PAYPAL").amount(99_000L)
                .status(PaymentStatus.PENDING).subpack(pack).user(user)
                .payload("{\"usd\":\"" + usd + "\"}").build();
    }

    private void givenPending(PaymentMomo payment) {
        when(paymentRepo.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));
    }

    // ---------- đổi tiền ----------

    @Test
    void vndIsConvertedToUsdAtTheFixedRateRoundedToCents() {
        assertThat(PaypalPaymentService.toUsd(BigDecimal.valueOf(99_000), new BigDecimal("25835.5"))).isEqualByComparingTo("3.83");
        assertThat(PaypalPaymentService.toUsd(BigDecimal.valueOf(249_000), new BigDecimal("25835.5"))).isEqualByComparingTo("9.64");
    }

    @Test
    void aTinyAmountStillCostsAtLeastOneCent() {
        assertThat(PaypalPaymentService.toUsd(BigDecimal.ONE, new BigDecimal("25835.5"))).isEqualByComparingTo("0.01");
    }

    // ---------- tạo đơn ----------

    @Test
    void createOrderSendsTheUsdAmountAndStoresAPendingOrder() {
        when(client.createOrder(any(), anyString(), anyString(), anyString()))
                .thenReturn(new PaypalClient.Order(ORDER_ID, "https://www.sandbox.paypal.com/checkoutnow?token=" + ORDER_ID));

        String approveUrl = service.createOrder(userId, packId);

        assertThat(approveUrl).isEqualTo("https://www.sandbox.paypal.com/checkoutnow?token=" + ORDER_ID);
        verify(rateLimiter).check(RateLimiter.Limit.PAYMENT_CREATE, userId);
        verify(client).createOrder(eq(new BigDecimal("3.83")), eq("WeTube Premium - Basic Plan"),
                eq("https://api.example.test/api/payment/paypal/return"), eq("https://api.example.test/api/payment/paypal/cancel"));
        ArgumentCaptor<PaymentMomo> saved = ArgumentCaptor.forClass(PaymentMomo.class);
        verify(paymentRepo).save(saved.capture());
        assertThat(saved.getValue().getOrderId()).isEqualTo(ORDER_ID);
        assertThat(saved.getValue().getProvider()).isEqualTo("PAYPAL");
        assertThat(saved.getValue().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(saved.getValue().getAmount()).isEqualTo(99_000L);
        assertThat(saved.getValue().getUser()).isSameAs(user);
        assertThat(saved.getValue().getPayload()).contains("\"usd\":\"3.83\"");
    }

    @Test
    void notConfiguredIs503AndPayPalIsNotCalled() {
        ReflectionTestUtils.setField(config, "secret", "");

        assertThatThrownBy(() -> service.createOrder(userId, packId)).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        verify(client, never()).createOrder(any(), anyString(), anyString(), anyString());
    }

    @Test
    void payPalFailingToCreateTheOrderIs502WithItsReasonAndNothingIsStored() {
        when(client.createOrder(any(), anyString(), anyString(), anyString()))
                .thenThrow(new PaypalException("PayPal từ chối yêu cầu tạo đơn (INVALID_CLIENT)."));

        assertThatThrownBy(() -> service.createOrder(userId, packId)).isInstanceOfSatisfying(ResponseStatusException.class, e -> {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
            assertThat(e.getReason()).contains("INVALID_CLIENT");
        });
        verify(paymentRepo, never()).save(any());
    }

    @Test
    void unknownPackIs404() {
        UUID other = UUID.randomUUID();
        when(subPackRepo.findById(other)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createOrder(userId, other)).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void rateLimitIsEnforcedBeforeCallingPayPal() {
        doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "quá nhiều"))
                .when(rateLimiter).check(RateLimiter.Limit.PAYMENT_CREATE, userId);

        assertThatThrownBy(() -> service.createOrder(userId, packId)).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
        verify(client, never()).createOrder(any(), anyString(), anyString(), anyString());
    }

    // ---------- quay về từ PayPal ----------

    @Test
    void completedCaptureOfTheRightAmountGrantsThePackForItsDuration() {
        PaymentMomo payment = pending("3.83");
        givenPending(payment);
        when(client.capture(eq(ORDER_ID), anyString())).thenReturn(new PaypalClient.Capture("COMPLETED", "USD", new BigDecimal("3.83")));

        assertThat(service.completeReturn(ORDER_ID)).isTrue();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        PremiumUser granted = payment.getPremiumUser();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        assertThat(granted.getStartDate()).isEqualTo(today);
        assertThat(granted.getEndDate()).isEqualTo(today.plusDays(30));
        assertThat(granted.getUser()).isSameAs(user);
        verify(premiumRepo).saveAndFlush(any());
    }

    @Test
    void buyingWhileStillPremiumStartsAfterTheCurrentExpiry() {
        LocalDate expiry = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(10);
        when(premiumService.premiumUntil(userId)).thenReturn(Optional.of(expiry));
        PaymentMomo payment = pending("3.83");
        givenPending(payment);
        when(client.capture(eq(ORDER_ID), anyString())).thenReturn(new PaypalClient.Capture("COMPLETED", "USD", new BigDecimal("3.83")));

        service.completeReturn(ORDER_ID);

        assertThat(payment.getPremiumUser().getStartDate()).isEqualTo(expiry.plusDays(1));
        assertThat(payment.getPremiumUser().getEndDate()).isEqualTo(expiry.plusDays(31));
    }

    @Test
    void returningTwiceForTheSameOrderGrantsOnlyOnce() {
        PaymentMomo payment = pending("3.83");
        givenPending(payment);
        when(client.capture(eq(ORDER_ID), anyString())).thenReturn(new PaypalClient.Capture("COMPLETED", "USD", new BigDecimal("3.83")));

        assertThat(service.completeReturn(ORDER_ID)).isTrue();
        PremiumUser first = payment.getPremiumUser();
        assertThat(service.completeReturn(ORDER_ID)).isTrue();

        assertThat(payment.getPremiumUser()).isSameAs(first);
        verify(premiumRepo, times(1)).saveAndFlush(any());
        verify(client, times(1)).capture(anyString(), anyString());
    }

    @Test
    void aDifferentAmountIsFailedAndGrantsNothing() {
        PaymentMomo payment = pending("3.83");
        givenPending(payment);
        when(client.capture(eq(ORDER_ID), anyString())).thenReturn(new PaypalClient.Capture("COMPLETED", "USD", new BigDecimal("0.01")));

        assertThat(service.completeReturn(ORDER_ID)).isFalse();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(premiumRepo, never()).saveAndFlush(any());
    }

    @Test
    void aDifferentCurrencyIsFailedAndGrantsNothing() {
        PaymentMomo payment = pending("3.83");
        givenPending(payment);
        when(client.capture(eq(ORDER_ID), anyString())).thenReturn(new PaypalClient.Capture("COMPLETED", "EUR", new BigDecimal("3.83")));

        assertThat(service.completeReturn(ORDER_ID)).isFalse();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(premiumRepo, never()).saveAndFlush(any());
    }

    @Test
    void aCaptureThatIsNotCompletedIsFailedAndGrantsNothing() {
        PaymentMomo payment = pending("3.83");
        givenPending(payment);
        when(client.capture(eq(ORDER_ID), anyString())).thenReturn(new PaypalClient.Capture("PENDING", "USD", new BigDecimal("3.83")));

        assertThat(service.completeReturn(ORDER_ID)).isFalse();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(premiumRepo, never()).saveAndFlush(any());
    }

    @Test
    void anOrderWithNoStoredAmountIsNeverGranted() {
        PaymentMomo payment = pending("3.83");
        payment.setPayload("{}");
        givenPending(payment);
        when(client.capture(eq(ORDER_ID), anyString())).thenReturn(new PaypalClient.Capture("COMPLETED", "USD", new BigDecimal("3.83")));

        assertThat(service.completeReturn(ORDER_ID)).isFalse();
        verify(premiumRepo, never()).saveAndFlush(any());
    }

    @Test
    void payPalErrorWhileCapturingKeepsTheOrderPendingSoItCanBeRetried() {
        PaymentMomo payment = pending("3.83");
        givenPending(payment);
        when(client.capture(eq(ORDER_ID), anyString())).thenThrow(new PaypalException("Không kết nối được PayPal"));

        assertThat(service.completeReturn(ORDER_ID)).isFalse();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(premiumRepo, never()).saveAndFlush(any());
    }

    @Test
    void anUnknownTokenOrAMoMoOrderIsNotCaptured() {
        when(paymentRepo.findByOrderIdForUpdate("nope")).thenReturn(Optional.empty());
        PaymentMomo momo = pending("3.83");
        momo.setProvider("MOMO");
        givenPending(momo);

        assertThat(service.completeReturn("nope")).isFalse();
        assertThat(service.completeReturn(ORDER_ID)).isFalse();
        assertThat(service.completeReturn(null)).isFalse();
        verify(client, never()).capture(anyString(), anyString());
    }

    @Test
    void anAlreadyFailedOrderIsNotCapturedAgain() {
        PaymentMomo payment = pending("3.83");
        payment.setStatus(PaymentStatus.FAILED);
        givenPending(payment);

        assertThat(service.completeReturn(ORDER_ID)).isFalse();
        verify(client, never()).capture(anyString(), anyString());
    }

    // ---------- huỷ ----------

    @Test
    void cancelFailsAPendingOrderOnly() {
        PaymentMomo payment = pending("3.83");
        givenPending(payment);

        service.cancel(ORDER_ID);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);

        PaymentMomo done = pending("3.83");
        done.setStatus(PaymentStatus.SUCCESS);
        givenPending(done);
        service.cancel(ORDER_ID);
        assertThat(done.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }
}
