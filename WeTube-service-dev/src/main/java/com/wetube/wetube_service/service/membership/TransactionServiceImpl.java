package com.wetube.wetube_service.service.membership;

import com.wetube.wetube_service.dto.MembershipPurchaseResponse;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.Transaction;
import com.wetube.wetube_service.entity.channel.Channel;
import com.wetube.wetube_service.entity.channel.MembershipTier;
import com.wetube.wetube_service.mapper.TransactionMapper;
import com.wetube.wetube_service.repository.MembershipTierRepository;
import com.wetube.wetube_service.repository.TransactionRepository;
import com.wetube.wetube_service.repository.UserRepository;
import com.wetube.wetube_service.repository.channel.ChannelRepository;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Builder
public class TransactionServiceImpl implements TransactionService {

    private final UserRepository userRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final ChannelRepository channelRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    @Override
    @Transactional
    public MembershipPurchaseResponse purchaseMembership(UUID userId, UUID membershipId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        MembershipTier membership = membershipTierRepository.findById(membershipId)
                .orElseThrow(() -> new RuntimeException("Membership not found"));
        Channel channel = membership.getChannel();

        BigDecimal amount = BigDecimal.valueOf(membership.getPrice());
        BigDecimal wetubeFee = amount.multiply(BigDecimal.valueOf(0.1));
        BigDecimal channelRevenue = amount.subtract(wetubeFee);

        // update channel revenue
        channel.setRevenue(channel.getRevenue() + channelRevenue.floatValue());
        channelRepository.save(channel);

        // save transaction
        Transaction transaction = Transaction.builder()
                .user(user)
                .membership(membership)
                .channel(channel)
                .amount(amount)
                .channelRevenue(channelRevenue)
                .wetubeFee(wetubeFee)
                .build();
        transactionRepository.save(transaction);

        return transactionMapper.toDto(transaction);
    }
}
