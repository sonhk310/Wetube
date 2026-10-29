package com.wetube.wetube_service.service.premium.paypal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import com.wetube.wetube_service.entity.channel.Channel;
import com.wetube.wetube_service.entity.channel.MembershipTier;
import com.wetube.wetube_service.entity.premium.PaymentMomo;
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

/** Mua hội viên kênh bằng PayPal (M1): đơn chỉ cho cấp trả phí đang nhận của kênh người khác; chỉ cấp hội viên khi tiền đúng. */
class PaypalMembershipOrderTest {

    private static final String ORDER_ID = "7MB190127TN364715";

    private final PaypalConfiguration config = new PaypalConfiguration();
    private final PaypalClient client = mock(PaypalClient.class);
    private final PaymentMomoRepository paymentRepo = mock(PaymentMomoRepository.class);
    private final UserRepository userRepo = mock(UserRepository.class);
    private final MemberTierRepository tierRepo = mock(MemberTierRepository.class);
    private final ChannelMembershipService membershipService = mock(ChannelMembershipService.class);
    private final PremiumUserRepository premiumRepo = mock(PremiumUserRepository.class);
    private final PremiumUserService premiumService = mock(PremiumUserService.class);

    private PaypalPaymentService service;
    private AppUser fan;
    private AppUser owner;
    private Channel channel;
    private MembershipTier tier;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(config, "clientId", "id");
        ReflectionTestUtils.setField(config, "secret", "secret");
        ReflectionTestUtils.setField(config, "baseUrl", "https://api-m.sandbox.paypal.com");
        ReflectionTestUtils.setField(config, "returnBase", "https://api.example.test/api/payment/paypal/");
        ReflectionTestUtils.setField(config, "vndPerUsd", new BigDecimal("25835.5"));
        service = new PaypalPaymentService(config, client, paymentRepo, mock(SubPackRepository.class), userRepo,
                new PremiumGranter(premiumRepo, premiumService), membershipService, tierRepo, mock(RateLimiter.class));

        fan = AppUser.builder().id(UUID.randomUUID()).email("fan@x.y").build();
        owner = AppUser.builder().id(UUID.randomUUID()).email("owner@x.y").build();
        channel = Channel.builder().id(UUID.randomUUID()).name("Kênh thử").handle("kenh").user(owner).build();
        tier = MembershipTier.builder().id(UUID.randomUUID()).title("Vàng").price(50_000).channel(channel).build();
        when(userRepo.findById(fan.getId())).thenReturn(Optional.of(fan));
        when(userRepo.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(tierRepo.findById(tier.getId())).thenReturn(Optional.of(tier));
    }

    private void expectStatus(HttpStatus status, Runnable call) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(status));
        verify(client, never()).createOrder(any(), anyString(), anyString(), anyString());
    }

    @Test
    void orderIsPricedFromTheTierAndRemembersWhichTierWasBought() {
        when(client.createOrder(any(), anyString(), anyString(), anyString()))
                .thenReturn(new PaypalClient.Order(ORDER_ID, "https://www.sandbox.paypal.com/checkoutnow?token=" + ORDER_ID));

        String url = service.createMembershipOrder(fan.getId(), tier.getId());

        assertThat(url).endsWith(ORDER_ID);
        verify(client).createOrder(eq(new BigDecimal("1.94")), eq("WeTube - Hội viên Kênh thử - Vàng"), anyString(), anyString());
        ArgumentCaptor<PaymentMomo> saved = ArgumentCaptor.forClass(PaymentMomo.class);
        verify(paymentRepo).save(saved.capture());
        assertThat(saved.getValue().getTier()).isSameAs(tier);
        assertThat(saved.getValue().getSubpack()).isNull();
        assertThat(saved.getValue().getAmount()).isEqualTo(50_000L);
        assertThat(saved.getValue().getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void defaultArchivedAndFreeTiersCannotBeBought() {
        tier.setDefaultTier(true);
        expectStatus(HttpStatus.BAD_REQUEST, () -> service.createMembershipOrder(fan.getId(), tier.getId()));
        tier.setDefaultTier(false);
        tier.setArchived(true);
        expectStatus(HttpStatus.BAD_REQUEST, () -> service.createMembershipOrder(fan.getId(), tier.getId()));
        tier.setArchived(false);
        tier.setPrice(0);
        expectStatus(HttpStatus.BAD_REQUEST, () -> service.createMembershipOrder(fan.getId(), tier.getId()));
    }

    @Test
    void channelOwnerCannotJoinTheirOwnChannel() {
        expectStatus(HttpStatus.BAD_REQUEST, () -> service.createMembershipOrder(owner.getId(), tier.getId()));
    }

    @Test
    void unknownTierIsNotFound() {
        expectStatus(HttpStatus.NOT_FOUND, () -> service.createMembershipOrder(fan.getId(), UUID.randomUUID()));
    }

    @Test
    void paidMembershipOrderGrantsMembershipNotPremium() {
        PaymentMomo payment = PaymentMomo.builder().id(UUID.randomUUID()).orderId(ORDER_ID).provider("PAYPAL").amount(50_000L)
                .status(PaymentStatus.PENDING).tier(tier).user(fan).payload("{\"usd\":\"1.94\"}").build();
        when(paymentRepo.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));
        when(client.capture(eq(ORDER_ID), anyString())).thenReturn(new PaypalClient.Capture("COMPLETED", "USD", new BigDecimal("1.94")));

        assertThat(service.completeReturn(ORDER_ID)).isTrue();

        verify(membershipService).grant(payment);
        verify(premiumRepo, never()).saveAndFlush(any());
        verify(paymentRepo).save(payment);
    }

    @Test
    void wrongAmountGrantsNothing() {
        PaymentMomo payment = PaymentMomo.builder().id(UUID.randomUUID()).orderId(ORDER_ID).provider("PAYPAL").amount(50_000L)
                .status(PaymentStatus.PENDING).tier(tier).user(fan).payload("{\"usd\":\"1.94\"}").build();
        when(paymentRepo.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));
        when(client.capture(eq(ORDER_ID), anyString())).thenReturn(new PaypalClient.Capture("COMPLETED", "USD", new BigDecimal("0.50")));

        assertThat(service.completeReturn(ORDER_ID)).isFalse();

        verify(membershipService, never()).grant(any());
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void returnPageKnowsWhichChannelWasJoined() {
        PaymentMomo payment = PaymentMomo.builder().orderId(ORDER_ID).provider("PAYPAL").tier(tier).user(fan).build();
        when(paymentRepo.findByOrderId(ORDER_ID)).thenReturn(Optional.of(payment));

        assertThat(service.membershipChannelOf(ORDER_ID)).isSameAs(channel);
        assertThat(service.membershipChannelOf("khac")).isNull();
        assertThat(service.membershipChannelOf(null)).isNull();
    }
}
