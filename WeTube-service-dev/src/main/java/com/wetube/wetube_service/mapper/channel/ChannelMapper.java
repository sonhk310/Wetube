package com.wetube.wetube_service.mapper.channel;

import com.wetube.wetube_service.dto.response.ChannelResponseDto;
import com.wetube.wetube_service.dto.response.SubscribedChannelResponseDto;
import com.wetube.wetube_service.dto.response.UserChannelResponseDto;
import com.wetube.wetube_service.entity.channel.Channel;
import com.wetube.wetube_service.entity.channel.Subscription;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {MemberTierMapper.class})
public interface ChannelMapper {

    // User's Channel
    UserChannelResponseDto toChannelDto(Channel channel);



    ChannelResponseDto toChannelResponseDto(Channel channel);
}
