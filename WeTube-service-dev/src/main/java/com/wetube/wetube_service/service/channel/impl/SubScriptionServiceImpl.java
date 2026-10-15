package com.wetube.wetube_service.service.channel.impl;

import com.wetube.wetube_service.mapper.channel.SubscriptionMapper;
import com.wetube.wetube_service.repository.channel.ChannelRepository;
import com.wetube.wetube_service.repository.channel.MemberTierRepository;
import com.wetube.wetube_service.repository.channel.SubscriptionRepository;
import com.wetube.wetube_service.repository.UserRepository;
import com.wetube.wetube_service.dto.request.SubscriptionRequest;
import com.wetube.wetube_service.dto.request.UnsubscribeRequest;
import com.wetube.wetube_service.dto.response.SubscribedChannelResponseDto;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.channel.Channel;
import com.wetube.wetube_service.entity.channel.MembershipTier;
import com.wetube.wetube_service.entity.channel.Subscription;
import com.wetube.wetube_service.entity.compositekey.SubscriptionId;
import com.wetube.wetube_service.enumeration.SubscriptionType;
import com.wetube.wetube_service.exception.ResourceNotFoundException;
import com.wetube.wetube_service.service.PlaylistService;
import com.wetube.wetube_service.service.channel.SubscriptionService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@AllArgsConstructor
@Transactional
public class SubScriptionServiceImpl implements SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;
    private final MemberTierRepository memberTierRepository;
    private final SubscriptionMapper subscriptionMapper;
    private final PlaylistService playlistService;

    @Override
    public List<SubscribedChannelResponseDto> getSubscribedChannels(UUID userId) {
        Optional.ofNullable(userId)
                .orElseThrow(() -> new IllegalArgumentException("UserID is empty"));

        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User","Id", userId.toString());
        }

        List<Subscription> subscriptions = Optional.ofNullable(subscriptionRepository.findById_Subscriber_Id(userId))
                .orElseGet(Collections::emptyList);

        return subscriptions.stream()
                .sorted((s1, s2) -> s2.getCreatedAt().compareTo(s1.getCreatedAt()))
                .map(s -> subscriptionMapper.toSubChannelDto(s, playlistService))
                .toList();
    }

    @Override
    public void updateSubscriptionMode(UUID subscriberId, UUID channelId, UUID tierId, SubscriptionType newMode) {
        SubscriptionId subscriptionId = resolveSubscriptionId(subscriberId, channelId, tierId);

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", subscriptionId.toString()));

        subscription.setNotificationMode(newMode);
        subscriptionRepository.save(subscription);
    }

    @Override
    public void subscribe(SubscriptionRequest req) {
        SubscriptionId subscriptionId = resolveSubscriptionId(req.getSubscriberId(), req.getChannelId(),req.getTierId());

        if (subscriptionRepository.existsById(subscriptionId)) {
            throw new IllegalStateException("User already subscribed this channel/tier");
        }

        Subscription subscription = new Subscription(
                subscriptionId,
                SubscriptionType.PERSONALIZE, // Default
                null
        );
        subscriptionRepository.save(subscription);

        Channel channel = subscriptionId.getTier().getChannel();
        channel.setTotalSubscribers(channel.getTotalSubscribers() + 1);
        channelRepository.save(channel);
    }

    @Override
    public void unsubscribe(UnsubscribeRequest req) {
        AppUser subscriber = userRepository.findById(req.getSubscriberId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", req.getSubscriberId().toString()));

        MembershipTier tier = memberTierRepository.findById(req.getTierId())
                .orElseThrow(() -> new ResourceNotFoundException("MembershipTier", "id", req.getTierId().toString()));

        SubscriptionId subscriptionId = new SubscriptionId(subscriber, tier);

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", subscriptionId.toString()));

        subscriptionRepository.delete(subscription);

        Channel channel = tier.getChannel();
        channel.setTotalSubscribers(Math.max(0, channel.getTotalSubscribers() - 1));
        channelRepository.save(channel);
    }

    private SubscriptionId resolveSubscriptionId(UUID subscriberId, UUID channelId, UUID tierId) {
        AppUser subscriber = userRepository.findById(subscriberId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", subscriberId.toString()));

        MembershipTier tier;
        if (tierId == null) {
            Channel channel = channelRepository.findById(channelId)
                    .orElseThrow(() -> new ResourceNotFoundException("Channel", "id", channelId.toString()));

            tier = channel.getMembershipTiers().stream()
                    .filter(MembershipTier::isDefaultTier)
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("MembershipTier", "channelId", channel.getId().toString()));
        } else {
            tier = memberTierRepository.findById(tierId)
                    .orElseThrow(() -> new ResourceNotFoundException("MembershipTier", "id", tierId.toString()));
        }

        return new SubscriptionId(subscriber, tier);
    }
}
