package com.wetube.wetube_service.service.channel.impl;

import com.wetube.wetube_service.dto.response.MemberTierResponseDto;
import com.wetube.wetube_service.entity.channel.Channel;
import com.wetube.wetube_service.entity.channel.MembershipTier;
import com.wetube.wetube_service.exception.DefaultTierException;
import com.wetube.wetube_service.exception.DuplicateResourceException;
import com.wetube.wetube_service.exception.ResourceNotFoundException;
import com.wetube.wetube_service.mapper.channel.MemberTierMapper;
import com.wetube.wetube_service.repository.channel.ChannelRepository;
import com.wetube.wetube_service.repository.channel.MemberTierRepository;
import com.wetube.wetube_service.service.channel.MemberTierService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.wetube.wetube_service.constants.AppConsts.MAX_TIER;

@Service
@AllArgsConstructor
@Transactional
public class MemberTierServiceImpl implements MemberTierService {
    private final ChannelRepository channelRepository;
    private final MemberTierRepository tierRepository;
    private final MemberTierMapper mapper;

    @Override
    public UUID addTier(UUID channelId, MemberTierResponseDto dto) {

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", "id", channelId.toString()));

        int currentTierCount = channel.getMembershipTiers().size();
        if (currentTierCount >= MAX_TIER) {
            throw new DefaultTierException(String.format("Cannot add more than %d tiers per channel, including the default tier",MAX_TIER - 1));
        }

        boolean exists = channel.getMembershipTiers().stream()
                .anyMatch(t -> t.getTitle().equalsIgnoreCase(dto.getTitle())
                        || t.getPrice() == dto.getPrice());

        if (exists) {
            throw new DuplicateResourceException("Tier title or price already exist in this channel");
        }

        MembershipTier tier = mapper.toEntity(dto);
        tier.setChannel(channel);
        tier.setDefaultTier(false);

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

        Channel channel = tier.getChannel();

        boolean exists = channel.getMembershipTiers().stream()
                .filter(t -> !t.getId().equals(tierId)) // Remove current tier
                .anyMatch(t -> t.getTitle().equalsIgnoreCase(dto.getTitle())
                        || t.getPrice() == dto.getPrice());

        if (exists) {throw new DuplicateResourceException("Tier title or price already exists in this channel");}

        mapper.updateEntity(dto,tier);
        tier.setDefaultTier(false);
        tierRepository.save(tier);
    }

    @Override
    public void deleteTier(UUID tierId) {
        MembershipTier tier = tierRepository.findById(tierId)
                .orElseThrow(() -> new ResourceNotFoundException("MembershipTier", "id", tierId.toString()));

        if (tier.isDefaultTier()) {
            throw new DefaultTierException("Can't delete default tier");
        }

        tierRepository.delete(tier);
    }
}
