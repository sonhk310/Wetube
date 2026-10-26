package com.wetube.wetube_service.service.video;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import com.wetube.wetube_service.dto.video.VideoDto;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.mapper.video.VideoMapper;
import com.wetube.wetube_service.repository.PlaylistRepository;
import com.wetube.wetube_service.repository.UserRepository;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.service.CloudinaryService;
import com.wetube.wetube_service.service.VideoIndexer;
import com.wetube.wetube_service.service.notification.NotificationService;
import com.wetube.wetube_service.service.video.impl.VideoServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VideoDraftTest {

    @Mock VideoRepository videoRepository;
    @Mock UserRepository userRepository;
    @Mock PlaylistRepository playlistRepository;
    @Mock CloudinaryService cloudinaryService;
    @Mock VideoIndexer videoIndexer;
    @Mock NotificationService notificationService;
    @Mock VideoMapper videoMapper;

    @InjectMocks VideoServiceImpl videoService;

    private final UUID userId = UUID.randomUUID();
    private final MockMultipartFile file =
            new MockMultipartFile("videoFile", "clip.mp4", "video/mp4", new byte[] {1, 2, 3});

    @BeforeEach
    void setUp() {
        AppUser user = new AppUser();
        user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(playlistRepository.findByUserIdAndPlaylistType(any(), any())).thenReturn(Optional.empty());
        when(videoRepository.save(any(Video.class))).thenAnswer(inv -> {
            Video v = inv.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });
        when(videoMapper.toDto(any(Video.class))).thenReturn(new VideoDto());
    }

    private Video savedVideo() {
        ArgumentCaptor<Video> captor = ArgumentCaptor.forClass(Video.class);
        verify(videoRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void draftIsPrivateTitledAfterFileAndNotIndexedOrNotified() throws Exception {
        when(cloudinaryService.uploadVideoWithMeta(file))
                .thenReturn(new CloudinaryService.UploadedVideo("https://x/v.mp4", 1080, 1920, 45f));

        videoService.createDraft(file, 45f, 1080, 1920, userId);

        Video saved = savedVideo();
        assertThat(saved.getVideosStatus()).isEqualTo(ActiveStatus.DRAFT);
        assertThat(saved.getTitle()).isEqualTo("clip");
        assertThat(saved.isShort()).isTrue();
        assertThat(saved.getVideoUrl()).isEqualTo("https://x/v.mp4");
        verify(videoIndexer, never()).index(any());
        verify(notificationService, never()).notifyNewVideo(any(), any(), any(), anyBoolean());
    }

    @Test
    void clientSizeWinsOverCloudinarySize() throws Exception {
        // Video quay dọc có cờ xoay: Cloudinary có thể báo khung gốc nằm ngang, trình duyệt hiện dọc
        when(cloudinaryService.uploadVideoWithMeta(file))
                .thenReturn(new CloudinaryService.UploadedVideo("https://x/v.mp4", 1920, 1080, 45f));

        videoService.createDraft(file, 45f, 1080, 1920, userId);

        assertThat(savedVideo().isShort()).isTrue();
    }

    @Test
    void cloudinarySizeIsUsedWhenClientHasNone() throws Exception {
        when(cloudinaryService.uploadVideoWithMeta(file))
                .thenReturn(new CloudinaryService.UploadedVideo("https://x/v.mp4", 1080, 1920, 45f));

        videoService.createDraft(file, null, null, null, userId);

        assertThat(savedVideo().isShort()).isTrue();
    }

    @Test
    void clientSizeIsUsedWhenCloudinaryHasNone() throws Exception {
        when(cloudinaryService.uploadVideoWithMeta(file))
                .thenReturn(new CloudinaryService.UploadedVideo("https://x/v.mp4", null, null, null));

        videoService.createDraft(file, 60f, 1920, 1080, userId);

        Video saved = savedVideo();
        assertThat(saved.isShort()).isFalse();
        assertThat(saved.getDuration()).isEqualTo(60f);
    }
}
