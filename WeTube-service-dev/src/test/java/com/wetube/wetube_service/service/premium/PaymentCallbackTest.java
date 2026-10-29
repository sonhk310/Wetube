package com.wetube.wetube_service.service.premium;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.wetube.wetube_service.configuration.MomoConfiguration;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.premium.PaymentMomo;
import com.wetube.wetube_service.entity.premium.PremiumUser;
import com.wetube.wetube_service.entity.premium.SubPack;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.enumeration.PaymentStatus;
import com.wetube.wetube_service.repository.UserRepository;
import com.wetube.wetube_service.repository.premium.PaymentMomoRepository;
import com.wetube.wetube_service.repository.premium.PremiumUserRepository;
import com.wetube.wetube_service.repository.premium.SubPackRepository;
import com.wetube.wetube_service.service.premium.impl.PaymentServiceImpl;
import com.wetube.wetube_service.utility.HmacSHA256Util;

/** Kết quả thanh toán MoMo (/ipn và /redirect): chỉ callback có chữ ký đúng mới được cấp gói, và chỉ một lần. */
class PaymentCallbackTest {

    private static final String ACCESS_KEY = "testAccessKey";
    private static final String SECRET_KEY = "testSecretKey";
    private static final String ORDER_ID = "order-1";

    private final MomoConfiguration momo = new MomoConfiguration();
    private final PaymentMomoRepository paymentRepo = mock(PaymentMomoRepository.class);
    private final PremiumUserRepository premiumRepo = mock(PremiumUserRepository.class);
    private final PremiumUserService premiumService = mock(PremiumUserService.class);
    private PaymentServiceImpl service;
    private PaymentMomo payment;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(momo, "accessKey", ACCESS_KEY);
        ReflectionTestUtils.setField(momo, "secretKey", SECRET_KEY);
        service = new PaymentServiceImpl(momo, paymentRepo, mock(SubPackRepository.class),
                mock(UserRepository.class), new PremiumGranter(premiumRepo, premiumService));
        ReflectionTestUtils.setField(service, "verifySignature", true);

        SubPack pack = SubPack.builder().id(UUID.randomUUID()).name("Basic Plan")
                .price(BigDecimal.valueOf(99_000)).durationDays(30).build();
        payment = PaymentMomo.builder().orderId(ORDER_ID).amount(99_000L).status(PaymentStatus.PENDING)
                .subpack(pack).user(AppUser.builder().id(UUID.randomUUID()).email("a@b.c").build()).build();
        when(paymentRepo.findByOrderId(ORDER_ID)).thenReturn(Optional.of(payment));
    }

    /** Chuỗi ký theo tài liệu MoMo v2 (thứ tự chữ cái); viết lại độc lập với code chính. */
    private Map<String, String> signed(String resultCode) throws Exception {
        Map<String, String> p = new HashMap<>();
        p.put("partnerCode", "MOMO");
        p.put("orderId", ORDER_ID);
        p.put("requestId", "req-1");
        p.put("amount", "99000");
        p.put("orderInfo", "Thank you for your purchase at MoMo_test");
        p.put("orderType", "momo_wallet");
        p.put("transId", "4088878653");
        p.put("resultCode", resultCode);
        p.put("message", "Thành công.");
        p.put("payType", "qr");
        p.put("responseTime", "1698480000000");
        p.put("extraData", "eyJza3VzIjoiIn0=");
        String raw = "accessKey=" + ACCESS_KEY + "&amount=99000&extraData=" + p.get("extraData")
                + "&message=" + p.get("message") + "&orderId=" + ORDER_ID + "&orderInfo=" + p.get("orderInfo")
                + "&orderType=momo_wallet&partnerCode=MOMO&payType=qr&requestId=req-1&responseTime="
                + p.get("responseTime") + "&resultCode=" + resultCode + "&transId=4088878653";
        p.put("signature", HmacSHA256Util.sign(raw, SECRET_KEY));
        return p;
    }

    private static boolean confirmed(String response) {
        return response.contains("\"resultCode\": 0");
    }

    @Test
    void signedSuccessGrantsThePackForItsDuration() throws Exception {
        String response = service.handleMoMoIpn(signed("0"));

        assertThat(confirmed(response)).isTrue();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        PremiumUser granted = payment.getPremiumUser();
        assertThat(granted.getStatus()).isEqualTo(ActiveStatus.ACTIVE);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        assertThat(granted.getStartDate()).isEqualTo(today);
        assertThat(granted.getEndDate()).isEqualTo(today.plusDays(30));
        assertThat(granted.getUser()).isSameAs(payment.getUser());
    }

    @Test
    void buyingWhileStillPremiumStartsTheDayAfterTheCurrentExpiry() throws Exception {
        LocalDate expiry = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(10);
        when(premiumService.premiumUntil(payment.getUser().getId())).thenReturn(Optional.of(expiry));

        service.handleMoMoIpn(signed("0"));

        PremiumUser granted = payment.getPremiumUser();
        assertThat(granted.getStartDate()).isEqualTo(expiry.plusDays(1));
        assertThat(granted.getEndDate()).isEqualTo(expiry.plusDays(31));
    }

    @Test
    void tamperedAmountIsRejectedAndNothingIsGranted() throws Exception {
        Map<String, String> p = signed("0");
        p.put("amount", "1");

        String response = service.handleMoMoIpn(p);

        assertThat(confirmed(response)).isFalse();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(premiumRepo, never()).saveAndFlush(any());
    }

    @Test
    void successFlippedFromFailureWithoutResigningIsRejected() throws Exception {
        Map<String, String> p = signed("1006");
        p.put("resultCode", "0");

        service.handleMoMoIpn(p);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(premiumRepo, never()).saveAndFlush(any());
    }

    @Test
    void missingOrWrongSignatureIsRejected() throws Exception {
        Map<String, String> none = signed("0");
        none.remove("signature");
        Map<String, String> wrong = signed("0");
        wrong.put("signature", "0".repeat(64));

        service.handleMoMoIpn(none);
        service.handleMoMoIpn(wrong);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(premiumRepo, never()).saveAndFlush(any());
    }

    @Test
    void signedCallbackFromAnotherSecretIsRejected() throws Exception {
        ReflectionTestUtils.setField(momo, "secretKey", "someoneElsesKey");

        service.handleMoMoIpn(signed("0"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void ipnAndRedirectForTheSameOrderGrantOnlyOnce() throws Exception {
        service.handleMoMoIpn(signed("0"));
        PremiumUser first = payment.getPremiumUser();

        String again = service.handleMoMoIpn(signed("0"));

        assertThat(confirmed(again)).isTrue();
        assertThat(payment.getPremiumUser()).isSameAs(first);
        verify(premiumRepo, times(1)).saveAndFlush(any());
    }

    @Test
    void signedFailureMarksFailedAndGrantsNothing() throws Exception {
        service.handleMoMoIpn(signed("1006"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getPremiumUser()).isNull();
        verify(premiumRepo, never()).saveAndFlush(any());
    }

    @Test
    void unknownOrderIsNotConfirmed() throws Exception {
        when(paymentRepo.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());

        assertThat(confirmed(service.handleMoMoIpn(signed("0")))).isFalse();
    }
}
