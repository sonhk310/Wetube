package com.wetube.wetube_service.dto.response;

import com.wetube.wetube_service.dto.response.playlist.UserPlaylistDto;

import java.util.List;

public record MeResponseDto(
        String sub,
        String email,
        String picture,
        String name,
        List<String> roles,
        String channelId,
        UserChannelResponseDto channel,
        List<UserPlaylistDto> playlists
) { }
