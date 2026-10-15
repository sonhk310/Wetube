package com.wetube.wetube_service.service.channel.impl;

import com.wetube.wetube_service.dto.response.ChannelResponseDto;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.channel.Channel;
import com.wetube.wetube_service.entity.channel.ChannelCategory;
import com.wetube.wetube_service.entity.channel.MembershipTier;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.enumeration.CategoryType;
import com.wetube.wetube_service.exception.ChannelAlreadyExistsException;
import com.wetube.wetube_service.exception.ResourceNotFoundException;
import com.wetube.wetube_service.mapper.channel.ChannelMapper;
import com.wetube.wetube_service.repository.channel.ChannelCategoryRepository;
import com.wetube.wetube_service.repository.channel.ChannelRepository;
import com.wetube.wetube_service.repository.UserRepository;
import com.wetube.wetube_service.service.channel.ChannelService;
import com.wetube.wetube_service.service.impl.GeoIPService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@AllArgsConstructor
@Service
@Transactional
public class ChannelServiceImpl implements ChannelService {
    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;
    private final ChannelMapper channelMapper;

    private final ChannelCategoryRepository categoryRepository;
    private final GeoIPService geoIPService;

    @Override
    public ChannelResponseDto getChannelById(UUID channelId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", "id", channelId.toString()));

        return channelMapper.toChannelResponseDto(channel);
    }

    @Override
    public void initiateChannel(UUID userID, String ip) {
        AppUser user = userRepository.findById(userID)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userID.toString()));

        if (user.getChannel() != null) {
            throw new ChannelAlreadyExistsException("User with id " + userID + " already has a channel.");
        }

        String countryCode = geoIPService.getCountryCode(ip).orElse("UN"); // UN = Unknown
        Channel channel = Channel.builder()
                .name(user.getName() != null ? user.getName() : "New Channel")
                .picture(user.getPicture())
                .status(ActiveStatus.ACTIVE)
                .countryCode(countryCode)
                .totalSubscribers(0)
                .totalVideos(0)
                .totalViews(0)
                .revenue(0f)
                .user(user)
                .build();

        List<ChannelCategory> defaultCategories = new ArrayList<>();
        CategoryType[] categoryTypes = CategoryType.values();
        for (int i = 0; i < categoryTypes.length; i++) {
            ChannelCategory category = ChannelCategory.builder()
                    .categoryType(categoryTypes[i])
                    .orderPosition(i + 1)
                    .isVisible(true)
                    .channel(channel)
                    .build();
            defaultCategories.add(category);
        }

        MembershipTier defaultTier = MembershipTier.builder()
                .title(user.getName() + " Default Tier")
                .description("Default membership tier")
                .price(0f)
                .defaultTier(true)
                .channel(channel)
                .build();

        channel.setCategories(defaultCategories);
        channel.setMembershipTiers(List.of(defaultTier));
        user.setChannel(channel);

        userRepository.save(user);
    }
}