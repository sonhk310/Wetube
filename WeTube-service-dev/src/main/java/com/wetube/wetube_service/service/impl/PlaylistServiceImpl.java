package com.wetube.wetube_service.service.impl;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
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
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.service.PlaylistService;

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

    @Override
    public UserPlaylistDto createPlaylist(CreatePlaylistRequest dto){
        AppUser user = userRepo.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("user", "id", dto.getUserId().toString()));

        Playlist playlist = playlistMapper.toEntity(dto, user);
        Playlist saved = playlistRepo.save(playlist);
        playlistRepo.save(playlist);

        return playlistMapper.toDto(saved);
    }

    @Override
    public List<UserPlaylistDto> getAllPlaylistByUserId(UUID userId) {
        List<Playlist> playlists = playlistRepo.findByUser_Id(userId);

        if (!userRepo.existsById(userId)) {
            throw new ResourceNotFoundException("userId", "id", userId.toString());
        }
        return playlistMapper.toDtoList(playlists);
    }

    @Override
    public List<UserPlaylistDto> getUserPlaylistById(UUID userId) {
        List<Playlist> playlists = playlistRepo.findByUser_Id(userId);

        // Remove USER_UPLOADED
        List<Playlist> filtered = playlists.stream()
                .filter(p -> p.getPlaylistType() != PlaylistType.USER_UPLOADED)
                .toList();

        return playlistMapper.toDtoList(filtered);
    }
     @Override
    public List<UserPlaylistDto> getAllPlaylistByTypeUserId(UUID userId, PlaylistType playlistType) {
        List<Playlist> playlists = playlistRepo.findByUser_IdAndPlaylistType(userId, playlistType);
        if (playlists.isEmpty()) {
            throw new ResourceNotFoundException("userId exists or playlistType error", "id", userId.toString());
        }
        return playlistMapper.toDtoList(playlists);
    }

    @Override
    public List<UserPlaylistDto> getUserCreatedPlaylistById(UUID userId) {
        List<Playlist> playlists = playlistRepo.findByUser_Id(userId);

        List<Playlist> filtered = playlists.stream()
                .filter(p -> p.getPlaylistType() == PlaylistType.USER_PLAYLIST)
                .toList();

        return playlistMapper.toDtoList(filtered);
    }

    @Override
    public PlaylistDetailDto getPlaylistDetailedById(UUID playlistId) {
        Playlist playlist = playlistRepo.findById(playlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Playlist", "id", playlistId.toString()));

//        return playlistMapper.toPlaylistDetailDto(playlist);
        List<PlaylistVideoDetailDto> videoDetails = playlist.getPlaylistVideos().stream()
                .map(playlistVideo -> {
                    Video video = playlistVideo.getVideo();
                    AppUser user = video.getUser();

                    // 3. Build DTO mới với đầy đủ thông tin
                    return PlaylistVideoDetailDto.builder()
                            .videoId(video.getId())
                            .videoTitle(video.getTitle())
                            .videoUrl(video.getVideoUrl())
                            .thumbnailUrl(video.getThumbnailUrl())
                            .duration(video.getDuration())
                            .totalView(video.getTotalView())
                            .description(video.getDescription())
                            .createdAt(video.getCreatedAt() != null ? video.getCreatedAt().toLocalDate() : null)
                            .updatedAt(video.getUpdatedAt() != null ? video.getUpdatedAt().toLocalDate() : null)
                            .build();
                })
                .collect(Collectors.toList());

        return PlaylistDetailDto.builder()
                .playlistId(playlist.getId())
                .playlistTitle(playlist.getTitle())
                .playlistType(playlist.getPlaylistType())
                .totalVideos(videoDetails.size())
                .createdAt(playlist.getCreatedAt() != null ? playlist.getCreatedAt().toLocalDate().atStartOfDay() : null)
                .videos(videoDetails)
                .build();
    }

    @Override
    public PlaylistDetailDto getPlaylistDetailedById(UUID channelId, String playlistName) {
        Optional<Channel> opt = channelRepo.findById(channelId);
        opt.orElseThrow(() -> new ResourceNotFoundException("Channel", "id", channelId.toString()));

        UUID userID = opt.get().getUserId();

        Playlist playlist = playlistRepo.findByUser_IdAndTitle(userID, playlistName)
                .orElseThrow(() -> new ResourceNotFoundException("Playlist", "title", playlistName));
        return playlistMapper.toPlaylistDetailDto(playlist);
    }


    @Override
    public String getTopViewUserUploaded(UUID userId) {
        List<Playlist> playlists = playlistRepo.findByUser_IdAndPlaylistType(userId, PlaylistType.USER_UPLOADED);
        Optional<Video> mostViewedVideo = playlists.
                getFirst().getPlaylistVideos().
                stream() .map(PlaylistVideo::getVideo).
                max(Comparator.comparingInt(Video::getTotalView));
        return mostViewedVideo.get().getVideoUrl();
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

    List<PlaylistVideo> playlistVideos = playlistVideoRepo.findByPlaylist_IdOrderByUpdatedAtDesc(history.getId());

    List<PlaylistVideoDetailDto> videos = playlistMapper.toPlaylistVideoDetailDtoList(playlistVideos);

            return playlistMapper.toPlaylistVideoDetailDtoList(playlistVideos);
}

@Override
public List<PlaylistVideoDetailDto> addVideoToHistory(UUID userId, UUID videoId) {
    List<Playlist> histories = playlistRepo.findByUser_IdAndPlaylistType(userId, PlaylistType.HISTORY);
    if (histories.isEmpty()) {
        throw new ResourceNotFoundException("Playlist", "type", "HISTORY");
    }
    Playlist history = histories.get(0);

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

}
