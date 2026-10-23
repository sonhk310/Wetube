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

    /** Video không được phép xem (riêng tư, bị ẩn, đã xoá...) bị lọc khỏi kết quả theo người xem. */
    PlaylistDetailDto getPlaylistDetailedById(UUID playlistId, UUID viewerId, boolean admin);

    PlaylistDetailDto getPlaylistDetailedById(UUID channelId, String playlistName, UUID viewerId, boolean admin);

    /** Id các playlist của user đang chứa video này (để hiện dấu tích ở hộp thoại Lưu). */
    List<UUID> getPlaylistIdsContaining(UUID userId, UUID videoId);

    void initiatePlaylist(UUID userId);

    PlaylistVideoDto addVideoToPlaylist(PlaylistaddRequest dto);

    void removeVideoFromPlaylist(UUID videoId, UUID playlistVideoId);

    void removePlaylist(UUID playlistId);

    String getTopViewUserUploaded(UUID userId);

    List<PlaylistVideo> findByPlaylist_IdOrderByUpdatedAtDesc(UUID playlistId);

    List<PlaylistVideoDetailDto> getHistoryByUser(UUID userId);

    List<PlaylistVideoDetailDto> addVideoToHistory(UUID userId, UUID videoId);

    /** Thêm video vào playlist hệ thống (LIKED, WATCH_LATER) của user; thêm lần nữa không tạo bản sao. */
    void addVideoToSystemPlaylist(UUID userId, UUID videoId, PlaylistType type);

    /** Bỏ video khỏi playlist hệ thống của user; không có thì bỏ qua. */
    void removeVideoFromSystemPlaylist(UUID userId, UUID videoId, PlaylistType type);

}
