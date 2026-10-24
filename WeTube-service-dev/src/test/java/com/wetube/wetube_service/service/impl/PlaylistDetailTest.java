package com.wetube.wetube_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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
}
