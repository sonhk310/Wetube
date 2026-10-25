package com.wetube.wetube_service.service.impl;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.wetube.wetube_service.dto.request.CreatePlaylistRequest;
import com.wetube.wetube_service.dto.request.PlaylistaddRequest;
import com.wetube.wetube_service.dto.response.playlist.PlaylistDetailDto;
import com.wetube.wetube_service.dto.response.playlist.PlaylistVideoDetailDto;
import com.wetube.wetube_service.dto.response.playlist.PlaylistVideoDto;
import com.wetube.wetube_service.dto.response.playlist.UserPlaylistDto;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.channel.Channel;
import com.wetube.wetube_service.entity.interaction.Comment;
import com.wetube.wetube_service.entity.playlist.Playlist;
import com.wetube.wetube_service.entity.playlist.PlaylistVideo;
import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.enumeration.PlaylistType;
import com.wetube.wetube_service.exception.ResourceNotFoundException;
import com.wetube.wetube_service.mapper.PlaylistMapper;
import com.wetube.wetube_service.repository.PlaylistRepository;
import com.wetube.wetube_service.repository.PlaylistVideoRepository;
import com.wetube.wetube_service.repository.UserRepository;
import com.wetube.wetube_service.repository.channel.ChannelRepository;
import com.wetube.wetube_service.repository.interaction.CommentRepository;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.security.VideoAccess;
import com.wetube.wetube_service.service.PlaylistService;
import com.wetube.wetube_service.service.RedisService;
import com.wetube.wetube_service.service.video.WatchProgressService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlaylistServiceImpl implements PlaylistService {

    private final PlaylistRepository playlistRepo;
    private final PlaylistVideoRepository playlistVideoRepo;
    private final VideoRepository videoRepo;
    private final UserRepository userRepo;
    private final ChannelRepository channelRepo;
    private final PlaylistMapper playlistMapper;
    private final RedisService redisService;
    private final CommentRepository commentRepository;

    @Override
    public UserPlaylistDto createPlaylist(CreatePlaylistRequest dto){
        AppUser user = userRepo.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("user", "id", dto.getUserId().toString()));

        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw new IllegalArgumentException("Tên danh sách phát không được để trống");
        }
        if (playlistRepo.findByUser_IdAndTitle(dto.getUserId(), dto.getTitle()).isPresent()) {
            throw new IllegalArgumentException("Bạn đã có danh sách phát tên \"" + dto.getTitle() + "\"");
        }

        Playlist playlist = playlistMapper.toEntity(dto, user);
        Playlist saved = playlistRepo.save(playlist);
        playlistRepo.save(playlist);

        return playlistMapper.toDto(saved);
    }

    @Override
    public List<UserPlaylistDto> getAllPlaylistByUserId(UUID userId, UUID viewerId, boolean admin) {
        List<Playlist> playlists = playlistRepo.findByUser_Id(userId);

        if (!userRepo.existsById(userId)) {
            throw new ResourceNotFoundException("userId", "id", userId.toString());
        }
        return toVisibleDtos(playlists, viewerId, admin);
    }

    @Override
    public List<UserPlaylistDto> getUserPlaylistById(UUID userId, UUID viewerId, boolean admin) {
        List<Playlist> playlists = playlistRepo.findByUser_Id(userId);

        // Remove USER_UPLOADED
        List<Playlist> filtered = playlists.stream()
                .filter(p -> p.getPlaylistType() != PlaylistType.USER_UPLOADED)
                .toList();

        return toVisibleDtos(filtered, viewerId, admin);
    }
     @Override
    public List<UserPlaylistDto> getAllPlaylistByTypeUserId(UUID userId, PlaylistType playlistType, UUID viewerId,
            boolean admin) {
        List<Playlist> playlists = playlistRepo.findByUser_IdAndPlaylistType(userId, playlistType);
        if (playlists.isEmpty()) {
            throw new ResourceNotFoundException("userId exists or playlistType error", "id", userId.toString());
        }
        return toVisibleDtos(playlists, viewerId, admin);
    }

    @Override
    public List<UserPlaylistDto> getUserCreatedPlaylistById(UUID userId, UUID viewerId, boolean admin) {
        List<Playlist> playlists = playlistRepo.findByUser_Id(userId);

        List<Playlist> filtered = playlists.stream()
                .filter(p -> p.getPlaylistType() == PlaylistType.USER_PLAYLIST)
                .toList();

        return toVisibleDtos(filtered, viewerId, admin);
    }

    /** Như toDtoList nhưng số video và ảnh bìa (video thêm sau cùng) chỉ tính video người xem được thấy. */
    private List<UserPlaylistDto> toVisibleDtos(List<Playlist> playlists, UUID viewerId, boolean admin) {
        return playlists.stream().map(playlist -> {
            UserPlaylistDto dto = playlistMapper.toDto(playlist);
            List<PlaylistVideo> visible = newestAddedFirst(visibleVideos(playlist, viewerId, admin));
            dto.setTotalVideos(visible.size());
            dto.setThumbnailUrl(visible.isEmpty() ? null : visible.get(0).getVideo().getThumbnailUrl());
            dto.setOwnerName(ownerChannelName(playlist));
            return dto;
        }).toList();
    }

    /** Video thêm vào danh sách gần nhất đứng đầu (thứ tự mặc định "Ngày thêm (mới nhất)"). */
    private static List<PlaylistVideo> newestAddedFirst(List<PlaylistVideo> playlistVideos) {
        return playlistVideos.stream()
                .sorted(Comparator.comparing(PlaylistVideo::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private static String ownerChannelName(Playlist playlist) {
        AppUser owner = playlist.getUser();
        return owner != null && owner.getChannel() != null ? owner.getChannel().getName() : null;
    }

    @Override
    public PlaylistDetailDto getPlaylistDetailedById(UUID playlistId, UUID viewerId, boolean admin) {
        Playlist playlist = playlistRepo.findById(playlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Playlist", "id", playlistId.toString()));

//        return playlistMapper.toPlaylistDetailDto(playlist);
        List<PlaylistVideo> all = playlist.getPlaylistVideos() == null ? List.of() : playlist.getPlaylistVideos();
        List<PlaylistVideo> visible = newestAddedFirst(visibleVideos(playlist, viewerId, admin));
        Map<UUID, Long> commentCounts = commentCounts(visible);
        List<PlaylistVideoDetailDto> videoDetails = visible.stream()
                .map(playlistVideo -> {
                    Video video = playlistVideo.getVideo();
                    AppUser user = video.getUser();
                    Channel channel = user != null ? user.getChannel() : null;

                    // 3. Build DTO mới với đầy đủ thông tin
                    return PlaylistVideoDetailDto.builder()
                            .videoId(video.getId())
                            .videoTitle(video.getTitle())
                            .channelName(channel != null ? channel.getName() : null)
                            .channelId(channel != null ? channel.getId() : null)
                            .videoUrl(video.getVideoUrl())
                            .thumbnailUrl(video.getThumbnailUrl())
                            .duration(video.getDuration())
                            .isShort(video.isShort())
                            .totalView(video.getTotalView())
                            .commentCount(commentCounts.getOrDefault(video.getId(), 0L))
                            .videoStatus(video.getVideosStatus() != null ? video.getVideosStatus().name() : null)
                            .description(video.getDescription())
                            .createdAt(video.getCreatedAt())
                            .updatedAt(video.getUpdatedAt())
                            .addedAt(playlistVideo.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());

        LocalDateTime lastAdded = visible.isEmpty() ? null : visible.get(0).getCreatedAt();
        return PlaylistDetailDto.builder()
                .playlistId(playlist.getId())
                .playlistTitle(playlist.getTitle())
                .playlistType(playlist.getPlaylistType())
                .totalVideos(videoDetails.size())
                .createdAt(playlist.getCreatedAt())
                .videos(videoDetails)
                .ownerId(playlist.getUser() != null ? playlist.getUser().getId() : null)
                .ownerName(ownerChannelName(playlist))
                .privacy(playlist.getPrivacy())
                .hiddenCount(all.size() - visible.size())
                .lastUpdatedAt(lastAdded != null ? lastAdded : playlist.getCreatedAt())
                .build();
    }

    @Override
    public PlaylistDetailDto getPlaylistDetailedById(UUID channelId, String playlistName, UUID viewerId, boolean admin) {
        Optional<Channel> opt = channelRepo.findById(channelId);
        opt.orElseThrow(() -> new ResourceNotFoundException("Channel", "id", channelId.toString()));

        UUID userID = opt.get().getUserId();

        Playlist playlist = playlistRepo.findByUser_IdAndTitle(userID, playlistName)
                .orElseThrow(() -> new ResourceNotFoundException("Playlist", "title", playlistName));
        List<PlaylistVideo> visible = visibleVideos(playlist, viewerId, admin);
        PlaylistDetailDto dto = playlistMapper.toPlaylistDetailDto(playlist);
        dto.setVideos(playlistMapper.toPlaylistVideoDetailDtoList(visible));
        dto.setTotalVideos(visible.size());
        return dto;
    }

    /** Số bình luận của các video trong playlist, đếm bằng một truy vấn. */
    private Map<UUID, Long> commentCounts(List<PlaylistVideo> playlistVideos) {
        if (playlistVideos.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = playlistVideos.stream().map(pv -> pv.getVideo().getId()).toList();
        return commentRepository.countByTargetIds(Comment.TargetType.VIDEO, ids).stream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));
    }

    /**
     * Video hiện trong playlist: công khai thì ai cũng thấy; không công khai chỉ chủ video / chủ playlist / admin
     * thấy; riêng tư và bị ẩn chỉ chủ video / admin thấy; đã xoá thì không ai thấy.
     */
    private List<PlaylistVideo> visibleVideos(Playlist playlist, UUID viewerId, boolean admin) {
        if (playlist.getPlaylistVideos() == null) {
            return List.of();
        }
        boolean playlistOwner = viewerId != null && playlist.getUser() != null
                && viewerId.equals(playlist.getUser().getId());
        return playlist.getPlaylistVideos().stream()
                .filter(pv -> VideoAccess.canSeeInList(pv.getVideo(), viewerId, admin)
                        || (playlistOwner && VideoAccess.canView(pv.getVideo(), viewerId, admin)))
                .toList();
    }

    @Override
    public List<UUID> getPlaylistIdsContaining(UUID userId, UUID videoId) {
        return playlistVideoRepo.findPlaylistIdsContaining(userId, videoId);
    }


    @Override
    public String getTopViewUserUploaded(UUID userId) {
        List<Playlist> playlists = playlistRepo.findByUser_IdAndPlaylistType(userId, PlaylistType.USER_UPLOADED);
        if (playlists.isEmpty() || playlists.getFirst().getPlaylistVideos() == null) {
            return null;
        }
        // Chỉ xét video đang công khai, không lộ video riêng tư / không công khai ra danh sách đăng ký
        Optional<Video> mostViewedVideo = playlists.
                getFirst().getPlaylistVideos().
                stream().map(PlaylistVideo::getVideo)
                .filter(v -> VideoAccess.isListed(v.getVideosStatus()))
                .max(Comparator.comparingInt(Video::getTotalView));
        return mostViewedVideo.map(Video::getVideoUrl).orElse(null);
    }


    @Override
    public void initiatePlaylist(UUID userID) {
        CreatePlaylistRequest history = new CreatePlaylistRequest("History",userID,PlaylistType.HISTORY,"public");
        CreatePlaylistRequest watchLater = new CreatePlaylistRequest("Watch Later",userID,PlaylistType.WATCH_LATER,"public");
        CreatePlaylistRequest liked = new CreatePlaylistRequest("Liked",userID,PlaylistType.LIKED,"private");
        CreatePlaylistRequest userVideos = new CreatePlaylistRequest("Uploaded",userID,PlaylistType.USER_UPLOADED,"public");
        createPlaylist(history);
        createPlaylist(watchLater);
        createPlaylist(liked);
        createPlaylist(userVideos);
    }

    @Override
    public PlaylistVideoDto addVideoToPlaylist(PlaylistaddRequest dto) {
        Playlist playlist = playlistRepo.findById(dto.getPlaylistId())
                .orElseThrow(() -> new ResourceNotFoundException("Playlist", "id", dto.getPlaylistId().toString()));
        Video video = videoRepo.findById(dto.getVideoId())
                .orElseThrow(() -> new ResourceNotFoundException("Video", "id", dto.getVideoId().toString()));

        PlaylistVideo pv = playlistMapper.toPlaylistVideo(dto, playlist, video);
        PlaylistVideo saved = playlistVideoRepo.save(pv);

        return playlistMapper.toPlaylistDto(saved);
    }

    @Override
    public void removeVideoFromPlaylist(UUID playlistId, UUID videoId) {
        PlaylistVideo playlistVideo = playlistVideoRepo.findByPlaylist_IdAndVideo_Id(playlistId, videoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PlaylistVideo",
                        "playlistId/videoId",
                        playlistId + " / " + videoId));

        playlistVideoRepo.delete(playlistVideo);
        // Xoá khỏi lịch sử thì quên luôn vị trí đang xem (giống YouTube)
        Playlist playlist = playlistVideo.getPlaylist();
        if (playlist.getPlaylistType() == PlaylistType.HISTORY) {
            redisService.delete(WatchProgressService.redisKey(playlist.getUser().getId(), videoId));
        }
    }


    @Override
    public void removePlaylist(UUID playlistId) {
        if (!playlistRepo.existsById(playlistId)) {
            throw new ResourceNotFoundException("Playlist", "id", playlistId.toString());
        }
        playlistRepo.deleteById(playlistId);
    }

@Override
public List<PlaylistVideo> findByPlaylist_IdOrderByUpdatedAtDesc(UUID playlistId) {
    return playlistVideoRepo.findByPlaylist_IdOrderByUpdatedAtDesc(playlistId);
}

@Override
public List<PlaylistVideoDetailDto> getHistoryByUser(UUID userId) {
    List<Playlist> histories = playlistRepo.findByUser_IdAndPlaylistType(userId, PlaylistType.HISTORY);

    if (histories.isEmpty()) {
        throw new ResourceNotFoundException("Playlist", "type", "HISTORY");
    }
    Playlist history = histories.get(0);

    List<PlaylistVideo> playlistVideos = playlistVideoRepo.findByPlaylist_IdOrderByUpdatedAtDesc(history.getId()).stream()
            .filter(pv -> VideoAccess.canView(pv.getVideo(), userId, false))
            .toList();

    return playlistMapper.toPlaylistVideoDetailDtoList(playlistVideos);
}

@Override
public List<PlaylistVideoDetailDto> addVideoToHistory(UUID userId, UUID videoId) {
    List<Playlist> histories = playlistRepo.findByUser_IdAndPlaylistType(userId, PlaylistType.HISTORY);
    if (histories.isEmpty()) {
        throw new ResourceNotFoundException("Playlist", "type", "HISTORY");
    }
    Playlist history = histories.get(0);

    if (isHistoryPaused(userId)) {
        return playlistMapper.toPlaylistVideoDetailDtoList(
                playlistVideoRepo.findByPlaylist_IdOrderByUpdatedAtDesc(history.getId()));
    }

    Video video = videoRepo.findById(videoId)
            .orElseThrow(() -> new ResourceNotFoundException("Video", "id", videoId.toString()));

    Optional<PlaylistVideo> existing = playlistVideoRepo.findByPlaylist_IdAndVideo_Id(history.getId(), videoId);

    if (existing.isPresent()) {
        PlaylistVideo pv = existing.get();
        pv.setUpdatedAt(LocalDateTime.now()); 
        playlistVideoRepo.save(pv);
    } else {
        PlaylistVideo newPv = playlistMapper.toPlaylistVideo(
                new PlaylistaddRequest(history.getId(), videoId, 0f),
                history,
                video
        );
        newPv.setUpdatedAt(LocalDateTime.now()); 
        playlistVideoRepo.save(newPv);
    }

    List<PlaylistVideo> playlistVideos = playlistVideoRepo.findByPlaylist_IdOrderByUpdatedAtDesc(history.getId());
    return playlistMapper.toPlaylistVideoDetailDtoList(playlistVideos);
}

@Override
public void removeFromHistory(UUID userId, UUID videoId) {
    playlistRepo.findByUser_IdAndPlaylistType(userId, PlaylistType.HISTORY).stream().findFirst()
            .flatMap(h -> playlistVideoRepo.findByPlaylist_IdAndVideo_Id(h.getId(), videoId))
            .ifPresent(playlistVideoRepo::delete);
    redisService.delete(WatchProgressService.redisKey(userId, videoId));
}

@Override
public void clearHistory(UUID userId) {
    for (Playlist history : playlistRepo.findByUser_IdAndPlaylistType(userId, PlaylistType.HISTORY)) {
        List<PlaylistVideo> entries = playlistVideoRepo.findByPlaylist_IdOrderByUpdatedAtDesc(history.getId());
        playlistVideoRepo.deleteAllInBatch(entries);
        entries.forEach(pv -> redisService.delete(WatchProgressService.redisKey(userId, pv.getVideo().getId())));
    }
}

@Override
public boolean isHistoryPaused(UUID userId) {
    return userRepo.findHistoryPaused(userId).orElse(false);
}

@Override
public void setHistoryPaused(UUID userId, boolean paused) {
    userRepo.setHistoryPaused(userId, paused);
}

@Override
public void addVideoToSystemPlaylist(UUID userId, UUID videoId, PlaylistType type) {
    List<Playlist> found = playlistRepo.findByUser_IdAndPlaylistType(userId, type);
    if (found.isEmpty()) {
        // user cũ có thể chưa có playlist hệ thống này: tạo bù
        String title = type == PlaylistType.LIKED ? "Liked" : "Watch Later";
        String visibility = type == PlaylistType.LIKED ? "private" : "public";
        createPlaylist(new CreatePlaylistRequest(title, userId, type, visibility));
        found = playlistRepo.findByUser_IdAndPlaylistType(userId, type);
    }
    Playlist playlist = found.get(0);

    if (playlistVideoRepo.findByPlaylist_IdAndVideo_Id(playlist.getId(), videoId).isPresent()) {
        return;
    }
    Video video = videoRepo.findById(videoId)
            .orElseThrow(() -> new ResourceNotFoundException("Video", "id", videoId.toString()));
    PlaylistVideo pv = playlistMapper.toPlaylistVideo(
            new PlaylistaddRequest(playlist.getId(), videoId, 0f), playlist, video);
    pv.setUpdatedAt(LocalDateTime.now());
    playlistVideoRepo.save(pv);
}

@Override
public void removeVideoFromSystemPlaylist(UUID userId, UUID videoId, PlaylistType type) {
    for (Playlist playlist : playlistRepo.findByUser_IdAndPlaylistType(userId, type)) {
        playlistVideoRepo.findByPlaylist_IdAndVideo_Id(playlist.getId(), videoId)
                .ifPresent(playlistVideoRepo::delete);
    }
}

}
