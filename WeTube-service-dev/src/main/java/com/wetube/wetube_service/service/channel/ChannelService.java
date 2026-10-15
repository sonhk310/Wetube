package com.wetube.wetube_service.service.channel;

import com.wetube.wetube_service.dto.response.ChannelResponseDto;
import com.wetube.wetube_service.entity.channel.Channel;

import java.util.UUID;

public interface ChannelService {
    ChannelResponseDto getChannelById(UUID channelId);
//    void updateChannel(UUID channelId, ChannelUpdateDto dto);
//    List<UserDto> getSubscribers(UUID channelId);
//    List<UserDto> getPremiumSubscribers(UUID channelId);
    void initiateChannel(UUID userID, String ip);
}
