package com.wetube.wetube_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.wetube.wetube_service.dto.response.playlist.PlaylistDetailDto;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.channel.Channel;
import com.wetube.wetube_service.entity.interaction.Comment;
import com.wetube.wetube_service.entity.playlist.Playlist;
import com.wetube.wetube_service.entity.playlist.PlaylistVideo;
import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.enumeration.PlaylistType;
import com.wetube.wetube_service.repository.PlaylistRepository;
import com.wetube.wetube_service.repository.interaction.CommentRepository;

@ExtendWith(MockitoExtension.class)
class PlaylistDetailTest {

    @Mock PlaylistRepository playlistRepo;
    @Mock CommentRepository commentRepository;
    @InjectMocks PlaylistServiceImpl playlistService;

    @Test
    void detailCarriesIsShortSoStudioCanSplitVideoAndShorts() {
        AppUser owner = new AppUser();
        owner.setId(UUID.randomUUID());
        Video shortVideo = new Video();
        shortVideo.setId(UUID.randomUUID());
        shortVideo.setUser(owner);
        shortVideo.setVideosStatus(ActiveStatus.DRAFT);
        shortVideo.setShort(true);
        Playlist playlist = Playlist.builder()
                .id(UUID.randomUUID())
                .title("Uploaded videos")
                .playlistType(PlaylistType.USER_UPLOADED)
                .user(owner)
                .build();
        playlist.setPlaylistVideos(List.of(PlaylistVideo.builder().playlist(playlist).video(shortVideo).build()));
        when(playlistRepo.findById(playlist.getId())).thenReturn(Optional.of(playlist));
        when(commentRepository.countByTargetIds(eq(Comment.TargetType.VIDEO), any()))
                .thenReturn(List.<Object[]>of(new Object[] {shortVideo.getId(), 4L}));

        PlaylistDetailDto detail = playlistService.getPlaylistDetailedById(playlist.getId(), owner.getId(), false);

        assertThat(detail.getVideos()).hasSize(1);
        assertThat(detail.getVideos().get(0).getIsShort()).isTrue();
        // Studio hiện số bình luận (gồm cả trả lời) trên thẻ video
        assertThat(detail.getVideos().get(0).getCommentCount()).isEqualTo(4L);
    }

    private static Video video(AppUser owner, ActiveStatus status) {
        Video v = new Video();
        v.setId(UUID.randomUUID());
        v.setUser(owner);
        v.setVideosStatus(status);
        return v;
    }

    private static PlaylistVideo added(Playlist playlist, Video video, LocalDateTime at) {
        return PlaylistVideo.builder().playlist(playlist).video(video).createdAt(at).build();
    }

    @Test
    void detailListsNewestAddedFirstWithOwnerAndHiddenCount() {
        Channel channel = Channel.builder().id(UUID.randomUUID()).name("Kỳ Sơn Huỳnh").build();
        AppUser owner = new AppUser();
        owner.setId(UUID.randomUUID());
        owner.setChannel(channel);
        AppUser other = new AppUser();
        other.setId(UUID.randomUUID());
        Video a = video(owner, ActiveStatus.ACTIVE);
        Video b = video(owner, ActiveStatus.ACTIVE);
        Video c = video(other, ActiveStatus.PRIVATE);
        Playlist playlist = Playlist.builder()
                .id(UUID.randomUUID())
                .title("Nhạc")
                .playlistType(PlaylistType.USER_PLAYLIST)
                .privacy("PUBLIC")
                .user(owner)
                .build();
        playlist.setPlaylistVideos(List.of(
                added(playlist, a, LocalDateTime.of(2026, 10, 1, 8, 0)),
                added(playlist, c, LocalDateTime.of(2026, 10, 2, 8, 0)),
                added(playlist, b, LocalDateTime.of(2026, 10, 3, 8, 0))));
        when(playlistRepo.findById(playlist.getId())).thenReturn(Optional.of(playlist));

        PlaylistDetailDto detail = playlistService.getPlaylistDetailedById(playlist.getId(), null, false);

        assertThat(detail.getVideos()).extracting("videoId").containsExactly(b.getId(), a.getId());
        assertThat(detail.getVideos().get(0).getAddedAt()).isEqualTo(LocalDateTime.of(2026, 10, 3, 8, 0));
        // Video riêng tư của người khác: không lộ ra, chỉ đếm
        assertThat(detail.getHiddenCount()).isEqualTo(1);
        assertThat(detail.getOwnerName()).isEqualTo("Kỳ Sơn Huỳnh");
        assertThat(detail.getOwnerId()).isEqualTo(owner.getId());
        assertThat(detail.getPrivacy()).isEqualTo("PUBLIC");
        assertThat(detail.getLastUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 10, 3, 8, 0));
    }

    @Test
    void emptyPlaylistLastUpdatedIsCreatedAt() {
        AppUser owner = new AppUser();
        owner.setId(UUID.randomUUID());
        Playlist playlist = Playlist.builder()
                .id(UUID.randomUUID())
                .title("Trống")
                .playlistType(PlaylistType.USER_PLAYLIST)
                .user(owner)
                .createdAt(LocalDateTime.of(2026, 9, 1, 8, 0))
                .build();
        playlist.setPlaylistVideos(List.of());
        when(playlistRepo.findById(playlist.getId())).thenReturn(Optional.of(playlist));

        PlaylistDetailDto detail = playlistService.getPlaylistDetailedById(playlist.getId(), owner.getId(), false);

        assertThat(detail.getLastUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 9, 1, 8, 0));
        assertThat(detail.getHiddenCount()).isZero();
        assertThat(detail.getOwnerName()).isNull();
    }

    @Test
    void detailCarriesChannelOfEachVideo() {
        Channel channel = Channel.builder().id(UUID.randomUUID()).name("Kỳ Sơn Huỳnh").build();
        AppUser owner = new AppUser();
        owner.setId(UUID.randomUUID());
        owner.setChannel(channel);
        Video video = new Video();
        video.setId(UUID.randomUUID());
        video.setUser(owner);
        video.setVideosStatus(ActiveStatus.ACTIVE);
        Playlist playlist = Playlist.builder()
                .id(UUID.randomUUID())
                .title("Nhạc")
                .playlistType(PlaylistType.USER_PLAYLIST)
                .user(owner)
                .build();
        playlist.setPlaylistVideos(List.of(PlaylistVideo.builder().playlist(playlist).video(video).build()));
        when(playlistRepo.findById(playlist.getId())).thenReturn(Optional.of(playlist));

        PlaylistDetailDto detail = playlistService.getPlaylistDetailedById(playlist.getId(), null, false);

        // Trang danh sách phát hiện tên kênh dưới mỗi video (trước đây luôn "Unknown Channel")
        assertThat(detail.getVideos().get(0).getChannelName()).isEqualTo("Kỳ Sơn Huỳnh");
        assertThat(detail.getVideos().get(0).getChannelId()).isEqualTo(channel.getId());
    }
}
