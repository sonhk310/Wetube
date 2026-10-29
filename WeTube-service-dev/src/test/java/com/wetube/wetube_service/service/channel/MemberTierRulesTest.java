package com.wetube.wetube_service.service.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.wetube.wetube_service.dto.response.MemberTierResponseDto;
import com.wetube.wetube_service.entity.channel.Channel;
import com.wetube.wetube_service.entity.channel.MembershipTier;
import com.wetube.wetube_service.exception.DefaultTierException;
import com.wetube.wetube_service.exception.DuplicateResourceException;
import com.wetube.wetube_service.mapper.channel.MemberTierMapper;
import com.wetube.wetube_service.repository.TransactionRepository;
import com.wetube.wetube_service.repository.channel.ChannelMemberRepository;
import com.wetube.wetube_service.repository.channel.ChannelRepository;
import com.wetube.wetube_service.repository.channel.MemberTierRepository;
import com.wetube.wetube_service.repository.premium.PaymentMomoRepository;
import com.wetube.wetube_service.service.channel.impl.MemberTierServiceImpl;

/** Luật cấp hội viên: tối đa 3 cấp trả phí, giá 10.000 – 500.000 ₫ (số nguyên), cấp đã có người mua chỉ ngừng nhận. */
class MemberTierRulesTest {

    private final ChannelRepository channelRepo = mock(ChannelRepository.class);
    private final MemberTierRepository tierRepo = mock(MemberTierRepository.class);
    private final MemberTierMapper mapper = mock(MemberTierMapper.class);
    private final ChannelMemberRepository memberRepo = mock(ChannelMemberRepository.class);
    private final TransactionRepository transactionRepo = mock(TransactionRepository.class);
    private final PaymentMomoRepository paymentRepo = mock(PaymentMomoRepository.class);

    private MemberTierServiceImpl service;
    private Channel channel;
    private MembershipTier defaultTier;

    @BeforeEach
    void setUp() {
        service = new MemberTierServiceImpl(channelRepo, tierRepo, mapper, memberRepo, transactionRepo, paymentRepo);
        channel = Channel.builder().id(UUID.randomUUID()).membershipTiers(new ArrayList<>()).build();
        defaultTier = MembershipTier.builder().id(UUID.randomUUID()).title("Mặc định").defaultTier(true).channel(channel).build();
        channel.getMembershipTiers().add(defaultTier);
        when(channelRepo.findById(channel.getId())).thenReturn(Optional.of(channel));
        when(mapper.toEntity(any())).thenAnswer(inv -> {
            MemberTierResponseDto d = inv.getArgument(0);
            return MembershipTier.builder().title(d.getTitle()).price(d.getPrice()).description(d.getDescription()).build();
        });
        when(tierRepo.save(any())).thenAnswer(inv -> {
            MembershipTier t = inv.getArgument(0);
            if (t.getId() == null) t.setId(UUID.randomUUID());
            return t;
        });
    }

    private static MemberTierResponseDto dto(String title, float price) {
        return MemberTierResponseDto.builder().title(title).price(price).description("quyền lợi").build();
    }

    private MembershipTier paid(String title, float price, boolean archived) {
        MembershipTier t = MembershipTier.builder().id(UUID.randomUUID()).title(title).price(price).archived(archived).channel(channel).build();
        channel.getMembershipTiers().add(t);
        when(tierRepo.findById(t.getId())).thenReturn(Optional.of(t));
        return t;
    }

    @Test
    void priceMustBeAWholeAmountBetweenTenAndFiveHundredThousand() {
        for (float bad : new float[] {0, 9_999, 500_001, 25_000.5f, -20_000}) {
            assertThatThrownBy(() -> service.addTier(channel.getId(), dto("Bạc", bad)))
                    .as("giá %s", bad).isInstanceOf(IllegalArgumentException.class);
        }
        service.addTier(channel.getId(), dto("Thấp nhất", 10_000));
        service.addTier(channel.getId(), dto("Cao nhất", 500_000));
        assertThat(channel.getMembershipTiers()).hasSize(3);
    }

    @Test
    void titleIsRequiredAndLimited() {
        assertThatThrownBy(() -> service.addTier(channel.getId(), dto("  ", 20_000))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.addTier(channel.getId(), dto("x".repeat(51), 20_000))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void atMostThreePaidTiersAreAccepted() {
        service.addTier(channel.getId(), dto("A", 20_000));
        service.addTier(channel.getId(), dto("B", 30_000));
        service.addTier(channel.getId(), dto("C", 40_000));

        assertThatThrownBy(() -> service.addTier(channel.getId(), dto("D", 50_000)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("3");
    }

    @Test
    void archivedTiersDoNotCountTowardsTheLimitOrThePriceCheck() {
        paid("Cũ 1", 20_000, true);
        paid("Cũ 2", 30_000, true);
        paid("B", 40_000, false);
        paid("C", 50_000, false);

        // Còn 2 cấp đang nhận nên thêm được một cấp nữa, kể cả trùng giá với cấp đã ngừng nhận
        service.addTier(channel.getId(), dto("Mới", 20_000));

        assertThatThrownBy(() -> service.addTier(channel.getId(), dto("Quá nhiều", 60_000))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void titleAndPriceMustBeUniqueAmongActiveTiers() {
        paid("Bạc", 20_000, false);

        assertThatThrownBy(() -> service.addTier(channel.getId(), dto("bạc", 30_000))).isInstanceOf(DuplicateResourceException.class);
        assertThatThrownBy(() -> service.addTier(channel.getId(), dto("Vàng", 20_000))).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void updateChecksTheSameRulesAndRefusesDefaultOrArchivedTiers() {
        MembershipTier silver = paid("Bạc", 20_000, false);
        paid("Vàng", 50_000, false);
        MembershipTier gone = paid("Cũ", 30_000, true);

        assertThatThrownBy(() -> service.updateTier(silver.getId(), dto("Bạc", 5_000))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.updateTier(silver.getId(), dto("Bạc", 50_000))).isInstanceOf(DuplicateResourceException.class);
        assertThatThrownBy(() -> service.updateTier(gone.getId(), dto("Cũ", 30_000))).isInstanceOf(IllegalArgumentException.class);
        when(tierRepo.findById(defaultTier.getId())).thenReturn(Optional.of(defaultTier));
        assertThatThrownBy(() -> service.updateTier(defaultTier.getId(), dto("Mặc định", 20_000))).isInstanceOf(DefaultTierException.class);

        // Giữ nguyên giá của chính mình là hợp lệ
        service.updateTier(silver.getId(), dto("Bạc mới", 20_000));
        verify(mapper).updateEntity(any(), any());
    }

    @Test
    void unusedTierIsDeletedForReal() {
        MembershipTier tier = paid("Bạc", 20_000, false);

        assertThat(service.deleteTier(tier.getId())).isFalse();

        verify(tierRepo).delete(tier);
    }

    @Test
    void tierThatWasBoughtIsOnlyArchived() {
        MembershipTier tier = paid("Bạc", 20_000, false);
        when(memberRepo.existsByTier_Id(tier.getId())).thenReturn(true);

        assertThat(service.deleteTier(tier.getId())).isTrue();

        assertThat(tier.isArchived()).isTrue();
        verify(tierRepo, never()).delete(any());
    }

    @Test
    void aTierWithOnlyAnOrderOrTransactionIsAlsoArchived() {
        MembershipTier withOrder = paid("Bạc", 20_000, false);
        MembershipTier withTransaction = paid("Vàng", 50_000, false);
        when(paymentRepo.existsByTier_Id(withOrder.getId())).thenReturn(true);
        when(transactionRepo.existsByMembership_Id(withTransaction.getId())).thenReturn(true);

        assertThat(service.deleteTier(withOrder.getId())).isTrue();
        assertThat(service.deleteTier(withTransaction.getId())).isTrue();
        verify(tierRepo, never()).delete(any());
    }

    @Test
    void defaultTierCannotBeDeleted() {
        when(tierRepo.findById(defaultTier.getId())).thenReturn(Optional.of(defaultTier));

        assertThatThrownBy(() -> service.deleteTier(defaultTier.getId())).isInstanceOf(DefaultTierException.class);
    }
}
