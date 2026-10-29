package com.wetube.wetube_service.service.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.wetube.wetube_service.dto.request.SubscriptionRequest;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.Transaction;
import com.wetube.wetube_service.entity.channel.Channel;
import com.wetube.wetube_service.entity.channel.ChannelMember;
import com.wetube.wetube_service.entity.channel.MembershipTier;
import com.wetube.wetube_service.entity.channel.Subscription;
import com.wetube.wetube_service.entity.premium.PaymentMomo;
import com.wetube.wetube_service.enumeration.PaymentStatus;
import com.wetube.wetube_service.repository.TransactionRepository;
import com.wetube.wetube_service.repository.UserRepository;
import com.wetube.wetube_service.repository.channel.ChannelMemberRepository;
import com.wetube.wetube_service.repository.channel.ChannelRepository;
import com.wetube.wetube_service.repository.channel.SubscriptionRepository;
import com.wetube.wetube_service.service.channel.SubscriptionService;

/** Hội viên kênh (M1): cấp hội viên khi PayPal đã thu tiền, cộng dồn, đổi cấp quy đổi ngày, huy hiệu. */
class ChannelMembershipServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 29);

    private final ChannelMemberRepository memberRepo = mock(ChannelMemberRepository.class);
    private final ChannelRepository channelRepo = mock(ChannelRepository.class);
    private final UserRepository userRepo = mock(UserRepository.class);
    private final TransactionRepository transactionRepo = mock(TransactionRepository.class);
    private final SubscriptionRepository subscriptionRepo = mock(SubscriptionRepository.class);
    private final SubscriptionService subscriptionService = mock(SubscriptionService.class);

    private ChannelMembershipService service;
    private AppUser fan;
    private Channel channel;
    private MembershipTier tier20k;
    private MembershipTier tier50k;

    @BeforeEach
    void setUp() {
        service = new ChannelMembershipService(memberRepo, channelRepo, userRepo, transactionRepo,
                subscriptionRepo, subscriptionService);
        ReflectionTestUtils.setField(service, "clock",
                Clock.fixed(TODAY.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(), ZoneId.of("Asia/Ho_Chi_Minh")));

        fan = AppUser.builder().id(UUID.randomUUID()).email("fan@x.y").build();
        channel = Channel.builder().id(UUID.randomUUID()).name("Kênh thử").handle("kenh").revenue(0).build();
        tier20k = tier("Bạc", 20_000);
        tier50k = tier("Vàng", 50_000);
        when(subscriptionRepo.findFirstById_Subscriber_IdAndId_Tier_Channel_Id(fan.getId(), channel.getId()))
                .thenReturn(Optional.empty());
    }

    private MembershipTier tier(String title, float price) {
        return MembershipTier.builder().id(UUID.randomUUID()).title(title).price(price).channel(channel).build();
    }

    private PaymentMomo payment(MembershipTier tier) {
        return PaymentMomo.builder().id(UUID.randomUUID()).provider("PAYPAL").status(PaymentStatus.PENDING)
                .user(fan).tier(tier).amount((long) tier.getPrice()).build();
    }

    private ChannelMember existing(MembershipTier tier, LocalDate start, LocalDate end) {
        return ChannelMember.builder().id(UUID.randomUUID()).user(fan).channel(channel).tier(tier)
                .startDate(start).endDate(end).build();
    }

    private ChannelMember savedMember() {
        ArgumentCaptor<ChannelMember> saved = ArgumentCaptor.forClass(ChannelMember.class);
        verify(memberRepo).save(saved.capture());
        return saved.getValue();
    }

    // ---------- quy đổi ngày ----------

    @Test
    void remainingDaysConvertByPriceRatio() {
        // Còn 10 ngày cấp 20.000 ₫ lên cấp 50.000 ₫ được 4 ngày
        assertThat(ChannelMembershipService.convertedDays(10, 20_000, 50_000)).isEqualTo(4);
        // Xuống cấp rẻ hơn thì nhiều ngày hơn
        assertThat(ChannelMembershipService.convertedDays(10, 50_000, 20_000)).isEqualTo(25);
    }

    @Test
    void convertedDaysAreCappedAndNeverNegative() {
        assertThat(ChannelMembershipService.convertedDays(30, 500_000, 10_000)).isEqualTo(365);
        assertThat(ChannelMembershipService.convertedDays(0, 20_000, 50_000)).isZero();
        assertThat(ChannelMembershipService.convertedDays(10, 20_000, 0)).isZero();
    }

    // ---------- cấp hội viên ----------

    @Test
    void firstPurchaseStartsTodayForThirtyDays() {
        PaymentMomo payment = payment(tier20k);
        when(memberRepo.findActive(fan.getId(), channel.getId(), TODAY)).thenReturn(List.of());

        service.grant(payment);

        ChannelMember member = savedMember();
        assertThat(member.getStartDate()).isEqualTo(TODAY);
        assertThat(member.getEndDate()).isEqualTo(TODAY.plusDays(30));
        assertThat(member.getTier()).isSameAs(tier20k);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getEventType()).isEqualTo("PAYMENT_SUCCESS");
    }

    @Test
    void sameTierExtendsAfterTheCurrentEndDate() {
        when(memberRepo.findActive(fan.getId(), channel.getId(), TODAY))
                .thenReturn(List.of(existing(tier20k, TODAY.minusDays(10), TODAY.plusDays(5))));

        service.grant(payment(tier20k));

        ChannelMember member = savedMember();
        assertThat(member.getStartDate()).isEqualTo(TODAY.plusDays(6));
        assertThat(member.getEndDate()).isEqualTo(TODAY.plusDays(36));
    }

    @Test
    void switchingTierStartsNowAndConvertsRemainingDays() {
        ChannelMember old = existing(tier20k, TODAY.minusDays(20), TODAY.plusDays(9)); // còn 10 ngày tính cả hôm nay
        when(memberRepo.findActive(fan.getId(), channel.getId(), TODAY)).thenReturn(List.of(old));

        service.grant(payment(tier50k));

        assertThat(old.getEndDate()).isEqualTo(TODAY.minusDays(1));
        ChannelMember member = savedMember();
        assertThat(member.getTier()).isSameAs(tier50k);
        assertThat(member.getStartDate()).isEqualTo(TODAY);
        assertThat(member.getEndDate()).isEqualTo(TODAY.plusDays(30 + 4));
    }

    @Test
    void switchingTierDropsQueuedFutureRowsOfTheOldTier() {
        ChannelMember current = existing(tier20k, TODAY.minusDays(1), TODAY.plusDays(29));
        ChannelMember queued = existing(tier20k, TODAY.plusDays(30), TODAY.plusDays(60));
        when(memberRepo.findActive(fan.getId(), channel.getId(), TODAY)).thenReturn(List.of(queued, current));

        service.grant(payment(tier50k));

        verify(memberRepo).delete(queued);
        assertThat(current.getEndDate()).isEqualTo(TODAY.minusDays(1));
    }

    @Test
    void transactionKeepsTenPercentForWeTubeAndCreditsTheChannel() {
        service.grant(payment(tier50k));

        ArgumentCaptor<Transaction> saved = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepo).save(saved.capture());
        Transaction t = saved.getValue();
        assertThat(t.getAmount()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(t.getWetubeFee()).isEqualByComparingTo(new BigDecimal("5000"));
        assertThat(t.getChannelRevenue()).isEqualByComparingTo(new BigDecimal("45000"));
        assertThat(channel.getRevenue()).isEqualTo(45_000f);
        verify(channelRepo).save(channel);
    }

    @Test
    void buyerBecomesASubscriberOnlyWhenNotYetOne() {
        service.grant(payment(tier20k));
        ArgumentCaptor<SubscriptionRequest> request = ArgumentCaptor.forClass(SubscriptionRequest.class);
        verify(subscriptionService).subscribe(request.capture());
        assertThat(request.getValue().getSubscriberId()).isEqualTo(fan.getId());
        assertThat(request.getValue().getChannelId()).isEqualTo(channel.getId());
        assertThat(request.getValue().getTierId()).isNull();
    }

    @Test
    void existingSubscriptionIsLeftAlone() {
        when(subscriptionRepo.findFirstById_Subscriber_IdAndId_Tier_Channel_Id(fan.getId(), channel.getId()))
                .thenReturn(Optional.of(new Subscription()));

        service.grant(payment(tier20k));

        verify(subscriptionService, never()).subscribe(any());
    }

    // ---------- tra hội viên ----------

    @Test
    void currentReturnsTheLatestActiveTierAndEndDate() {
        when(memberRepo.findActive(fan.getId(), channel.getId(), TODAY))
                .thenReturn(List.of(existing(tier50k, TODAY, TODAY.plusDays(12))));

        var membership = service.current(fan.getId(), channel.getId()).orElseThrow();

        assertThat(membership.tierTitle()).isEqualTo("Vàng");
        assertThat(membership.until()).isEqualTo(TODAY.plusDays(12));
        assertThat(service.current(null, channel.getId())).isEmpty();
        assertThat(service.current(UUID.randomUUID(), channel.getId())).isEmpty();
    }

    @Test
    void badgeTitleIsTheTierWithTheLongestRemainingTime() {
        UUID other = UUID.randomUUID();
        AppUser otherFan = AppUser.builder().id(other).email("o@x.y").build();
        when(memberRepo.findActiveOf(channel.getId(), List.of(fan.getId(), other), TODAY)).thenReturn(List.of(
                existing(tier20k, TODAY.minusDays(5), TODAY.plusDays(3)),
                existing(tier50k, TODAY, TODAY.plusDays(30)),
                ChannelMember.builder().user(otherFan).channel(channel).tier(tier20k).startDate(TODAY).endDate(TODAY.plusDays(9)).build()));

        Map<UUID, String> titles = service.tierTitles(channel.getId(), List.of(fan.getId(), other));

        assertThat(titles).containsEntry(fan.getId(), "Vàng").containsEntry(other, "Bạc").hasSize(2);
        assertThat(service.tierTitles(channel.getId(), List.of())).isEmpty();
    }
}
