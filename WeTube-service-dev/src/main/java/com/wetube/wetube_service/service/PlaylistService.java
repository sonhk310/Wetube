package com.wetube.wetube_service.service;

import java.util.List;
import java.util.UUID;

import com.wetube.wetube_service.dto.request.CreatePlaylistRequest;
import com.wetube.wetube_service.dto.request.PlaylistaddRequest;
import com.wetube.wetube_service.dto.response.playlist.PlaylistDetailDto;
import com.wetube.wetube_service.dto.response.playlist.PlaylistVideoDetailDto;
import com.wetube.wetube_service.dto.response.playlist.PlaylistVideoDto;
import com.wetube.wetube_service.dto.response.playlist.UserPlaylistDto;
import com.wetube.wetube_service.entity.playlist.PlaylistVideo;
import com.wetube.wetube_service.enumeration.PlaylistType;

public interface PlaylistService {
    UserPlaylistDto createPlaylist(CreatePlaylistRequest dto);

    List<UserPlaylistDto> getAllPlaylistByUserId(UUID userId);

    List<UserPlaylistDto> getUserPlaylistById(UUID userId);

    List<UserPlaylistDto> getUserCreatedPlaylistById(UUID userId);

    List<UserPlaylistDto> getAllPlaylistByTypeUserId(UUID userId, PlaylistType playlistType);

    PlaylistDetailDto getPlaylistDetailedById(UUID playlistVideoId);

    PlaylistDetailDto getPlaylistDetailedById(UUID channelId, String playlistName);

    void initiatePlaylist(UUID userId);

    PlaylistVideoDto addVideoToPlaylist(PlaylistaddRequest dto);

    void removeVideoFromPlaylist(UUID videoId, UUID playlistVideoId);

    void removePlaylist(UUID playlistId);

    String getTopViewUserUploaded(UUID userId);

    List<PlaylistVideo> findByPlaylist_IdOrderByUpdatedAtDesc(UUID playlistId);

    List<PlaylistVideoDetailDto> getHistoryByUser(UUID userId);

    List<PlaylistVideoDetailDto> addVideoToHistory(UUID userId, UUID videoId);

}
