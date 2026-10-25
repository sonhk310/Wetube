package com.wetube.wetube_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.wetube.wetube_service.dto.request.UpdatePlaylistRequest;
import com.wetube.wetube_service.dto.response.playlist.UserPlaylistDto;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.playlist.Playlist;
import com.wetube.wetube_service.entity.playlist.PlaylistVideo;
import com.wetube.wetube_service.enumeration.PlaylistType;
import com.wetube.wetube_service.mapper.PlaylistMapper;
import com.wetube.wetube_service.repository.PlaylistRepository;
import com.wetube.wetube_service.repository.PlaylistVideoRepository;

@ExtendWith(MockitoExtension.class)
class PlaylistEditTest {

    @Mock PlaylistRepository playlistRepo;
    @Mock PlaylistVideoRepository playlistVideoRepo;
    @Mock PlaylistMapper playlistMapper;
    @InjectMocks PlaylistServiceImpl playlistService;

    private final AppUser owner = new AppUser();

    @BeforeEach
    void setUp() {
        owner.setId(UUID.randomUUID());
        lenient().when(playlistMapper.toDto(any(Playlist.class))).thenAnswer(inv -> new UserPlaylistDto());
        lenient().when(playlistRepo.save(any(Playlist.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Playlist playlist(PlaylistType type, String title) {
        Playlist p = Playlist.builder().id(UUID.randomUUID()).title(title).playlistType(type).user(owner).build();
        p.setPlaylistVideos(List.of());
        lenient().when(playlistRepo.findById(p.getId())).thenReturn(Optional.of(p));
        return p;
    }

    @Test
    void updateRenamesAndChangesPrivacy() {
        Playlist p = playlist(PlaylistType.USER_PLAYLIST, "Cũ");

        playlistService.updatePlaylist(p.getId(), new UpdatePlaylistRequest("  Nhạc chill  ", "PRIVATE"));

        ArgumentCaptor<Playlist> saved = ArgumentCaptor.forClass(Playlist.class);
        verify(playlistRepo).save(saved.capture());
        assertThat(saved.getValue().getTitle()).isEqualTo("Nhạc chill");
        assertThat(saved.getValue().getPrivacy()).isEqualTo("PRIVATE");
    }

    @Test
    void updateRejectsSystemPlaylist() {
        Playlist p = playlist(PlaylistType.WATCH_LATER, "Watch Later");

        assertThatThrownBy(() -> playlistService.updatePlaylist(p.getId(), new UpdatePlaylistRequest("Xem sau", "PUBLIC")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Không sửa được danh sách phát hệ thống");
        verify(playlistRepo, never()).save(any());
    }

    @Test
    void updateRejectsBadTitleOrPrivacy() {
        Playlist p = playlist(PlaylistType.USER_PLAYLIST, "Cũ");

        assertThatThrownBy(() -> playlistService.updatePlaylist(p.getId(), new UpdatePlaylistRequest("   ", "PUBLIC")))
                .hasMessage("Tên danh sách phát phải có 1–150 ký tự");
        assertThatThrownBy(() -> playlistService.updatePlaylist(p.getId(), new UpdatePlaylistRequest("a".repeat(151), "PUBLIC")))
                .hasMessage("Tên danh sách phát phải có 1–150 ký tự");
        assertThatThrownBy(() -> playlistService.updatePlaylist(p.getId(), new UpdatePlaylistRequest("Mới", "FRIENDS")))
                .hasMessage("Chế độ hiển thị không hợp lệ");
        verify(playlistRepo, never()).save(any());
    }

    @Test
    void updateRejectsDuplicateTitle() {
        Playlist p = playlist(PlaylistType.USER_PLAYLIST, "Cũ");
        Playlist other = Playlist.builder().id(UUID.randomUUID()).title("Nhạc").playlistType(PlaylistType.USER_PLAYLIST).user(owner).build();
        when(playlistRepo.findByUser_IdAndTitle(owner.getId(), "Nhạc")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> playlistService.updatePlaylist(p.getId(), new UpdatePlaylistRequest("Nhạc", "PUBLIC")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Bạn đã có danh sách phát tên này");

        // Giữ tên, chỉ đổi chế độ: tên "trùng" với chính nó thì không lỗi
        when(playlistRepo.findByUser_IdAndTitle(owner.getId(), "Cũ")).thenReturn(Optional.of(p));
        playlistService.updatePlaylist(p.getId(), new UpdatePlaylistRequest("Cũ", "PRIVATE"));
        verify(playlistRepo).save(p);
    }

    @Test
    void removePlaylistRejectsSystemPlaylist() {
        Playlist p = playlist(PlaylistType.HISTORY, "History");

        assertThatThrownBy(() -> playlistService.removePlaylist(p.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Không xoá được danh sách phát hệ thống");
        verify(playlistRepo, never()).deleteById(any());
    }

    @Test
    void removeVideoRejectedForUploadsAndHistory() {
        for (PlaylistType type : List.of(PlaylistType.USER_UPLOADED, PlaylistType.HISTORY)) {
            Playlist p = playlist(type, type.name());
            UUID videoId = UUID.randomUUID();
            when(playlistVideoRepo.findByPlaylist_IdAndVideo_Id(p.getId(), videoId))
                    .thenReturn(Optional.of(PlaylistVideo.builder().playlist(p).build()));

            assertThatThrownBy(() -> playlistService.removeVideoFromPlaylist(p.getId(), videoId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Không xoá được video khỏi danh sách này");
        }
        verify(playlistVideoRepo, never()).delete(any());

        for (PlaylistType type : List.of(PlaylistType.WATCH_LATER, PlaylistType.USER_PLAYLIST)) {
            Playlist p = playlist(type, type.name());
            UUID videoId = UUID.randomUUID();
            PlaylistVideo pv = PlaylistVideo.builder().playlist(p).build();
            when(playlistVideoRepo.findByPlaylist_IdAndVideo_Id(p.getId(), videoId)).thenReturn(Optional.of(pv));

            playlistService.removeVideoFromPlaylist(p.getId(), videoId);
            verify(playlistVideoRepo).delete(pv);
        }
    }
}
