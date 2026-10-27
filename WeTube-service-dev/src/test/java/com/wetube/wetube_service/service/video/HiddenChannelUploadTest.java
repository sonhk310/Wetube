package com.wetube.wetube_service.service.video;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.wetube.wetube_service.dto.video.VideoDto;
import com.wetube.wetube_service.dto.video.VideoUpdateDto;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.channel.Channel;
import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.mapper.video.VideoMapper;
import com.wetube.wetube_service.repository.UserRepository;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.repository.video.VideoTagRepository;
import com.wetube.wetube_service.service.CloudinaryService;
import com.wetube.wetube_service.service.VideoIndexer;
import com.wetube.wetube_service.service.notification.NotificationService;
import com.wetube.wetube_service.service.video.impl.VideoServiceImpl;

/** Kênh bị admin ẩn: chủ kênh không tải lên, không đăng / công khai video được; vẫn đặt riêng tư được. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HiddenChannelUploadTest {

    @Mock VideoRepository videoRepository;
    @Mock UserRepository userRepository;
    @Mock CloudinaryService cloudinaryService;
    @Mock VideoIndexer videoIndexer;
    @Mock NotificationService notificationService;
    @Mock VideoMapper videoMapper;
    @Mock VideoTagRepository videoTagRepository;

    @InjectMocks VideoServiceImpl videoService;

    private final UUID userId = UUID.randomUUID();
    private final MockMultipartFile file = new MockMultipartFile("videoFile", "clip.mp4", "video/mp4", new byte[] {1, 2, 3});
    private AppUser user;
    private Video draft;

    @BeforeEach
    void setUp() {
        user = new AppUser();
        user.setId(userId);
        Channel channel = Channel.builder().id(UUID.randomUUID()).status(ActiveStatus.INACTIVE).user(user).build();
        user.setChannel(channel);
        draft = new Video();
        draft.setId(UUID.randomUUID());
        draft.setUser(user);
        draft.setVideosStatus(ActiveStatus.DRAFT);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(videoRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
        when(videoRepository.findByIdWithTags(draft.getId())).thenReturn(Optional.of(draft));
        when(videoRepository.findAllById(any())).thenReturn(List.of(draft));
        when(videoRepository.save(any(Video.class))).thenAnswer(inv -> inv.getArgument(0));
        when(videoMapper.toDto(any(Video.class))).thenReturn(new VideoDto());
    }

    private static void assertForbidden(Runnable call) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void uploadingADraftIsRejectedBeforeAnythingIsUploaded() throws Exception {
        assertForbidden(() -> videoService.createDraft(file, 45f, 1080, 1920, userId));
        verify(cloudinaryService, never()).uploadVideoWithMeta(any());
    }

    @Test
    void directUploadIsRejectedBeforeAnythingIsUploaded() throws Exception {
        VideoDto dto = new VideoDto();
        dto.setVideosStatus("PUBLIC");
        assertForbidden(() -> {
            try {
                videoService.createVideo(file, null, dto, userId);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
        verify(cloudinaryService, never()).uploadVideoWithMeta(any());
    }

    @Test
    void publishingOrUnlistingAnExistingVideoIsRejected() {
        for (String status : List.of("PUBLIC", "UNLISTED")) {
            assertForbidden(() -> videoService.updateVideo(draft.getId(),
                    VideoUpdateDto.builder().title("t").status(status).build(), null, userId));
            assertForbidden(() -> videoService.updateVisibility(List.of(draft.getId()), status, userId));
        }
        verify(videoIndexer, never()).index(any());
    }

    @Test
    void makingAVideoPrivateStillWorks() {
        draft.setVideosStatus(ActiveStatus.PUBLIC);
        videoService.updateVideo(draft.getId(), VideoUpdateDto.builder().title("t").status("PRIVATE").build(), null, userId);
        assertThat(draft.getVideosStatus()).isEqualTo(ActiveStatus.PRIVATE);
    }

    @Test
    void visibleChannelCanStillPublish() {
        user.getChannel().setStatus(ActiveStatus.ACTIVE);
        videoService.updateVideo(draft.getId(), VideoUpdateDto.builder().title("t").status("PUBLIC").build(), null, userId);
        // "PUBLIC" được lưu là ACTIVE (cả hai đều là công khai)
        assertThat(com.wetube.wetube_service.security.VideoAccess.isListed(draft.getVideosStatus())).isTrue();
    }
}
