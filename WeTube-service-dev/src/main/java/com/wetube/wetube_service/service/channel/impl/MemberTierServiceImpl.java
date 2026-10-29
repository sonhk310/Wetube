package com.wetube.wetube_service.service.channel.impl;
import com.wetube.wetube_service.dto.response.MemberTierResponseDto;
import com.wetube.wetube_service.entity.channel.Channel;
import com.wetube.wetube_service.entity.channel.MembershipTier;
import com.wetube.wetube_service.exception.DefaultTierException;
import com.wetube.wetube_service.exception.DuplicateResourceException;
import com.wetube.wetube_service.exception.ResourceNotFoundException;
import com.wetube.wetube_service.mapper.channel.MemberTierMapper;
import com.wetube.wetube_service.repository.TransactionRepository;
import com.wetube.wetube_service.repository.channel.ChannelMemberRepository;
import com.wetube.wetube_service.repository.channel.ChannelRepository;
import com.wetube.wetube_service.repository.channel.MemberTierRepository;
import com.wetube.wetube_service.repository.premium.PaymentMomoRepository;
import com.wetube.wetube_service.service.channel.MemberTierService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@AllArgsConstructor
@Transactional
public class MemberTierServiceImpl implements MemberTierService {

    /** Số cấp trả phí đang nhận hội viên tối đa của một kênh (không tính cấp mặc định và cấp đã ngừng nhận). */
    public static final int MAX_PAID_TIERS = 3;
    public static final float MIN_PRICE = 10_000f;
    public static final float MAX_PRICE = 500_000f;

    private final ChannelRepository channelRepository;
    private final MemberTierRepository tierRepository;
    private final MemberTierMapper mapper;
    private final ChannelMemberRepository memberRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentMomoRepository paymentRepository;

    /** Giá hội viên: số nguyên đồng trong khoảng cho phép. */
    static void validate(MemberTierResponseDto dto) {
        if (dto.getTitle() == null || dto.getTitle().isBlank() || dto.getTitle().trim().length() > 50) {
            throw new IllegalArgumentException("Tên cấp hội viên từ 1 đến 50 ký tự.");
        }
        if (dto.getDescription() != null && dto.getDescription().length() > 500) {
            throw new IllegalArgumentException("Mô tả quyền lợi tối đa 500 ký tự.");
        }
        float price = dto.getPrice();
        if (price < MIN_PRICE || price > MAX_PRICE || price != Math.floor(price)) {
            throw new IllegalArgumentException("Giá hội viên từ 10.000 đến 500.000 đồng, tính bằng số nguyên.");
        }
    }

    @Override
    public UUID addTier(UUID channelId, MemberTierResponseDto dto) {
        validate(dto);
        dto.setTitle(dto.getTitle().trim());
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", "id", channelId.toString()));

        long paidTiers = channel.getMembershipTiers().stream().filter(t -> !t.isDefaultTier() && !t.isArchived()).count();
        if (paidTiers >= MAX_PAID_TIERS) {
            throw new IllegalArgumentException("Mỗi kênh có tối đa " + MAX_PAID_TIERS + " cấp hội viên trả phí.");
        }
        if (duplicates(channel, null, dto)) {
            throw new DuplicateResourceException("Tier title or price already exist in this channel");
        }

        MembershipTier tier = mapper.toEntity(dto);
        tier.setChannel(channel);
        tier.setDefaultTier(false);
        tier.setArchived(false);
        MembershipTier saved = tierRepository.save(tier);
        channel.getMembershipTiers().add(saved);
        return saved.getId();
    }

    @Override
    public void updateTier(UUID tierId, MemberTierResponseDto dto) {
        MembershipTier tier = tierRepository.findById(tierId)
                .orElseThrow(() -> new ResourceNotFoundException("MembershipTier", "id", tierId.toString()));
        if (tier.isDefaultTier()) {
            throw new DefaultTierException("You can't update a default tier");
        }
        if (tier.isArchived()) {
            throw new IllegalArgumentException("Cấp này đã ngừng nhận hội viên, không sửa được.");
        }
        validate(dto);
        dto.setTitle(dto.getTitle().trim());
        if (duplicates(tier.getChannel(), tierId, dto)) {
            throw new DuplicateResourceException("Tier title or price already exists in this channel");
        }
        mapper.updateEntity(dto,tier);
        tier.setDefaultTier(false);
        tierRepository.save(tier);
    }

    /** Tên trùng với mọi cấp của kênh (kể cả đã ngừng nhận: khoá duy nhất của bảng), giá trùng với cấp đang nhận. */
    private static boolean duplicates(Channel channel, UUID exceptTierId, MemberTierResponseDto dto) {
        return channel.getMembershipTiers().stream()
                .filter(t -> !t.getId().equals(exceptTierId))
                .anyMatch(t -> t.getTitle().equalsIgnoreCase(dto.getTitle())
                        || (!t.isArchived() && t.getPrice() == dto.getPrice()));
    }

    /** Cấp đã có người mua (hội viên, giao dịch hay đơn thanh toán) thì chỉ ngừng nhận hội viên mới; trả true nếu vậy. */
    @Override
    public boolean deleteTier(UUID tierId) {
        MembershipTier tier = tierRepository.findById(tierId)
                .orElseThrow(() -> new ResourceNotFoundException("MembershipTier", "id", tierId.toString()));
        if (tier.isDefaultTier()) {
            throw new DefaultTierException("Can't delete default tier");
        }
        boolean used = memberRepository.existsByTier_Id(tierId)
                || transactionRepository.existsByMembership_Id(tierId)
                || paymentRepository.existsByTier_Id(tierId);
        if (used) {
            tier.setArchived(true);
            tierRepository.save(tier);
            return true;
        }
        tierRepository.delete(tier);
        return false;
    }
}
