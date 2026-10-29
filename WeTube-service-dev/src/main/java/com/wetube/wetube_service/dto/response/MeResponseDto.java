package com.wetube.wetube_service.dto.response;

import com.wetube.wetube_service.dto.response.playlist.UserPlaylistDto;

import java.time.LocalDate;
import java.util.List;

public record MeResponseDto(
        String sub,
        String email,
        String picture,
        String name,
        List<String> roles,
        String channelId,
        UserChannelResponseDto channel,
        List<UserPlaylistDto> playlists,
        /** Ngày hết hạn Premium (yyyy-MM-dd); null nếu không có Premium còn hiệu lực. */
        LocalDate premiumUntil
) {
    public static MeResponseDto withPremiumUntil(MeResponseDto me, LocalDate until) {
        return new MeResponseDto(me.sub, me.email, me.picture, me.name, me.roles, me.channelId, me.channel,
                me.playlists, until);
    }
}
