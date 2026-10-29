package com.wetube.wetube_service.service.membership;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wetube.wetube_service.dto.membership.ChannelTiersDto;
import com.wetube.wetube_service.dto.membership.MyMembershipDto;
import com.wetube.wetube_service.dto.membership.OwnerMembersDto;
import com.wetube.wetube_service.dto.membership.TierDto;
import com.wetube.wetube_service.dto.request.SubscriptionRequest;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.Transaction;
import com.wetube.wetube_service.entity.channel.Channel;
import com.wetube.wetube_service.entity.channel.ChannelMember;
import com.wetube.wetube_service.entity.channel.MembershipTier;
import com.wetube.wetube_service.entity.premium.PaymentMomo;
import com.wetube.wetube_service.enumeration.PaymentStatus;
import com.wetube.wetube_service.exception.ResourceNotFoundException;
import com.wetube.wetube_service.repository.TransactionRepository;
import com.wetube.wetube_service.repository.UserRepository;
import com.wetube.wetube_service.repository.channel.ChannelMemberRepository;
import com.wetube.wetube_service.repository.channel.ChannelRepository;
import com.wetube.wetube_service.repository.channel.SubscriptionRepository;
import com.wetube.wetube_service.service.channel.SubscriptionService;

import lombok.RequiredArgsConstructor;

/**
 * Hội viên kênh (M1): cấp hội viên sau khi PayPal thu tiền, tra hội viên của một người, dữ liệu cho Studio và huy hiệu.
 * Mỗi lần mua được {@link #DAYS_PER_PURCHASE} ngày, không tự gia hạn.
 */
@Service
@RequiredArgsConstructor
public class ChannelMembershipService {

    public static final int DAYS_PER_PURCHASE = 30;
    /** Phí WeTube giữ trên mỗi giao dịch hội viên. */
    static final BigDecimal WETUBE_FEE_RATE = new BigDecimal("0.10");
    /** Đổi cấp: số ngày quy đổi từ cấp cũ không vượt quá mức này (đổi từ cấp đắt sang cấp rẻ nhiều có thể ra rất nhiều ngày). */
    static final int MAX_CONVERTED_DAYS = 365;
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ChannelMemberRepository memberRepo;
    private final ChannelRepository channelRepo;
    private final UserRepository userRepo;
    private final TransactionRepository transactionRepo;
    private final SubscriptionRepository subscriptionRepo;
    private final SubscriptionService subscriptionService;

    private Clock clock = Clock.system(VIETNAM);

    /** Ngày còn lại của cấp cũ đổi sang cấp mới theo tỉ lệ giá: còn 10 ngày cấp 20.000 ₫, lên cấp 50.000 ₫ thì được 4 ngày. */
    static long convertedDays(long remainingDays, float oldPrice, float newPrice) {
        if (remainingDays <= 0 || oldPrice <= 0 || newPrice <= 0) {
            return 0;
        }
        return Math.min(MAX_CONVERTED_DAYS, Math.round(remainingDays * (double) oldPrice / newPrice));
    }

    /**
     * Cấp hội viên cho đơn đã thanh toán thành công: cùng cấp thì cộng dồn sau ngày hết hạn; khác cấp thì đổi ngay
     * (cấp cũ kết thúc hôm qua, ngày còn lại quy đổi theo giá). Ghi giao dịch (phí WeTube 10%), cộng doanh thu cho
     * kênh, tự đăng ký kênh nếu chưa. Người gọi lưu lại đơn (payment).
     */
    @Transactional
    public void grant(PaymentMomo payment) {
        MembershipTier tier = payment.getTier();
        Channel channel = tier.getChannel();
        AppUser user = payment.getUser();
        LocalDate today = LocalDate.now(clock);

        List<ChannelMember> active = memberRepo.findActive(user.getId(), channel.getId(), today);
        LocalDate start = today;
        long extraDays = 0;
        if (!active.isEmpty()) {
            ChannelMember latest = active.get(0);
            if (latest.getTier().getId().equals(tier.getId())) {
                start = latest.getEndDate().plusDays(1);
            } else {
                long remaining = ChronoUnit.DAYS.between(today, latest.getEndDate()) + 1;
                extraDays = convertedDays(remaining, latest.getTier().getPrice(), tier.getPrice());
                for (ChannelMember old : active) {
                    if (old.getStartDate().isAfter(today)) {
                        memberRepo.delete(old);
                    } else {
                        old.setEndDate(today.minusDays(1));
                    }
                }
            }
        }
        memberRepo.save(ChannelMember.builder()
                .user(user)
                .channel(channel)
                .tier(tier)
                .startDate(start)
                .endDate(start.plusDays(DAYS_PER_PURCHASE + extraDays))
                .build());

        BigDecimal amount = BigDecimal.valueOf((long) tier.getPrice());
        BigDecimal fee = amount.multiply(WETUBE_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal channelRevenue = amount.subtract(fee);
        transactionRepo.save(Transaction.builder()
                .user(user)
                .membership(tier)
                .channel(channel)
                .amount(amount)
                .channelRevenue(channelRevenue)
                .wetubeFee(fee)
                .build());
        channel.setRevenue(channel.getRevenue() + channelRevenue.floatValue());
        channelRepo.save(channel);

        // Hội viên cũng là người đăng ký kênh (cấp mặc định); đã đăng ký ở cấp nào thì giữ nguyên
        if (subscriptionRepo.findFirstById_Subscriber_IdAndId_Tier_Channel_Id(user.getId(), channel.getId()).isEmpty()) {
            subscriptionService.subscribe(new SubscriptionRequest(user.getId(), channel.getId(), null));
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setEventType("PAYMENT_SUCCESS");
    }

    /** Hội viên còn hạn của người này ở kênh (dòng hạn xa nhất). */
    @Transactional(readOnly = true)
    public Optional<MyMembershipDto> current(UUID userId, UUID channelId) {
        if (userId == null || channelId == null) {
            return Optional.empty();
        }
        return memberRepo.findActive(userId, channelId, LocalDate.now(clock)).stream().findFirst()
                .map(m -> new MyMembershipDto(m.getTier().getId(), m.getTier().getTitle(), m.getEndDate()));
    }

    /** Tên cấp của từng người đang là hội viên kênh (huy hiệu ở bình luận / chat); người không phải hội viên không có trong kết quả. */
    @Transactional(readOnly = true)
    public Map<UUID, String> tierTitles(UUID channelId, Collection<UUID> userIds) {
        if (channelId == null || userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, ChannelMember> latest = new HashMap<>();
        for (ChannelMember m : memberRepo.findActiveOf(channelId, userIds, LocalDate.now(clock))) {
            latest.merge(m.getUser().getId(), m, (a, b) -> a.getEndDate().isAfter(b.getEndDate()) ? a : b);
        }
        Map<UUID, String> titles = new HashMap<>();
        latest.forEach((userId, m) -> titles.put(userId, m.getTier().getTitle()));
        return titles;
    }

    /** Các cấp trả phí đang nhận hội viên (giá thấp trước) + hội viên hiện tại của người xem. */
    @Transactional(readOnly = true)
    public ChannelTiersDto tiersOf(UUID channelId, UUID viewerId) {
        Channel channel = channelRepo.findById(channelId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", "id", channelId.toString()));
        List<TierDto> tiers = channel.getMembershipTiers().stream()
                .filter(t -> !t.isDefaultTier() && !t.isArchived())
                .sorted(Comparator.comparingDouble(MembershipTier::getPrice))
                .map(t -> new TierDto(t.getId(), t.getTitle(), t.getPrice(), t.getDescription()))
                .toList();
        return new ChannelTiersDto(tiers, current(viewerId, channelId).orElse(null));
    }

    /** Studio của chủ kênh: các cấp (kể cả đã ngừng nhận) kèm số hội viên, hội viên còn hạn, doanh thu. */
    @Transactional(readOnly = true)
    public OwnerMembersDto ownerView(UUID ownerUserId) {
        AppUser owner = userRepo.findById(ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", ownerUserId.toString()));
        Channel channel = owner.getChannel();
        if (channel == null) {
            throw new ResourceNotFoundException("Channel", "user", ownerUserId.toString());
        }

        // Mỗi người một dòng: dòng còn hạn xa nhất
        Map<UUID, ChannelMember> latest = new LinkedHashMap<>();
        for (ChannelMember m : memberRepo.findActiveByChannel(channel.getId(), LocalDate.now(clock))) {
            latest.putIfAbsent(m.getUser().getId(), m);
        }
        Map<UUID, Integer> countByTier = new HashMap<>();
        latest.values().forEach(m -> countByTier.merge(m.getTier().getId(), 1, Integer::sum));

        List<OwnerMembersDto.OwnerTier> tiers = channel.getMembershipTiers().stream()
                .filter(t -> !t.isDefaultTier())
                .sorted(Comparator.comparingDouble(MembershipTier::getPrice))
                .map(t -> new OwnerMembersDto.OwnerTier(t.getId(), t.getTitle(), t.getPrice(), t.getDescription(),
                        t.isArchived(), countByTier.getOrDefault(t.getId(), 0)))
                .toList();

        List<OwnerMembersDto.MemberRow> members = new ArrayList<>();
        for (ChannelMember m : latest.values()) {
            AppUser u = m.getUser();
            members.add(new OwnerMembersDto.MemberRow(u.getId(), u.getName(), u.getPicture(),
                    u.getChannel() == null ? null : u.getChannel().getHandle(),
                    m.getTier().getTitle(), m.getStartDate(), m.getEndDate()));
            if (members.size() >= 500) {
                break;
            }
        }

        return new OwnerMembersDto(tiers, members, new OwnerMembersDto.Revenue(
                transactionRepo.totalAmount(channel.getId()),
                transactionRepo.totalFee(channel.getId()),
                transactionRepo.totalChannelRevenue(channel.getId())));
    }
}
