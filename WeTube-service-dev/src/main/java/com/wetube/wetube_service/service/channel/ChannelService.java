package com.wetube.wetube_service.service.channel;

import com.wetube.wetube_service.dto.response.ChannelResponseDto;
import com.wetube.wetube_service.dto.response.HandleCheckDto;
import com.wetube.wetube_service.entity.channel.Channel;

import java.util.UUID;

public interface ChannelService {
    ChannelResponseDto getChannelById(UUID channelId, UUID viewerId);
//    void updateChannel(UUID channelId, ChannelUpdateDto dto);
//    List<UserDto> getSubscribers(UUID channelId);
//    List<UserDto> getPremiumSubscribers(UUID channelId);
    void initiateChannel(UUID userID, String ip);

    ChannelResponseDto getChannelByHandle(String handle, UUID viewerId);

    /** Handle `raw` có dùng được cho kênh của người gọi không (đúng quy tắc, còn trống, hết thời gian chờ đổi). */
    HandleCheckDto checkHandle(UUID userId, String raw);

    /** Đổi handle kênh của người gọi; ném ResponseStatusException 400 / 409 / 429 khi không đổi được. */
    ChannelResponseDto changeHandle(UUID userId, String raw);
}
