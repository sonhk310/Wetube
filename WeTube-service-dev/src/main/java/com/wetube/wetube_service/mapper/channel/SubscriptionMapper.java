package com.wetube.wetube_service.mapper.channel;
import com.wetube.wetube_service.dto.response.SubscribedChannelResponseDto;
import com.wetube.wetube_service.entity.channel.Subscription;
import com.wetube.wetube_service.service.PlaylistService;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.UUID;


@Mapper(componentModel = "spring")
public interface SubscriptionMapper {
    // User's Subbed Channels
    @Mapping(target = "channelId", source = "id.tier.channel.id")
    @Mapping(target = "name", source = "id.tier.channel.name")
    @Mapping(target = "avatarUrl", source = "id.tier.channel.picture")
    @Mapping(target = "subscribers", source = "id.tier.channel.totalSubscribers")
    @Mapping(target = "description", source = "id.tier.channel.description")
    @Mapping(target = "videoUrl", source = ".", qualifiedByName = "mapVideoUrl")
    SubscribedChannelResponseDto toSubChannelDto(Subscription subscription, @Context PlaylistService playlistService);

    @Named("mapVideoUrl")
    default String getVideoUrl(Subscription subscription, @Context PlaylistService playlistService) {
        UUID userId = subscription.getId().getTier().getChannel().getUser().getId();
        return playlistService.getTopViewUserUploaded(userId);
    }
}
