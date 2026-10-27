package com.wetube.wetube_service.service.video;

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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import com.wetube.wetube_service.dto.video.VideoDto;
import com.wetube.wetube_service.dto.video.VideoUpdateDto;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.mapper.video.VideoMapper;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.repository.video.VideoTagRepository;
import com.wetube.wetube_service.service.CloudinaryService;
import com.wetube.wetube_service.service.VideoIndexer;
import com.wetube.wetube_service.service.notification.NotificationService;
import com.wetube.wetube_service.service.video.impl.VideoServiceImpl;

/** Đổi ảnh thu nhỏ của video: ảnh cũ bị xoá trên Cloudinary, trừ khi tải ảnh mới lỗi. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ThumbnailReplaceTest {

    private static final String OLD = "https://res.cloudinary.com/demo/image/upload/c_fill,h_200,w_300/old.webp";
    private static final String NEW = "https://res.cloudinary.com/demo/image/upload/c_fill,h_720,q_auto,w_1280/new.webp";

    @Mock VideoRepository videoRepository;
    @Mock VideoTagRepository videoTagRepository;
    @Mock VideoIndexer videoIndexer;
    @Mock NotificationService notificationService;
    @Mock VideoMapper videoMapper;
    @Mock CloudinaryService cloudinaryService;

    @Spy TopicCatalog topicCatalog = new TopicCatalog();

    @InjectMocks VideoServiceImpl videoService;

    private final UUID userId = UUID.randomUUID();
    private Video video;
    private final MockMultipartFile file = new MockMultipartFile("thumbnail", "t.png", "image/png", new byte[] { 1, 2, 3 });

    @BeforeEach
    void setUp() {
        AppUser user = new AppUser();
        user.setId(userId);
        video = new Video();
        video.setId(UUID.randomUUID());
        video.setUser(user);
        // Đã có chủ đề (C8), để test chỉ kiểm tra việc nó định kiểm tra
        video.setTopicId("music");
        video.setVideosStatus(ActiveStatus.PUBLIC);
        video.setThumbnailUrl(OLD);
        when(videoRepository.findById(video.getId())).thenReturn(Optional.of(video));
        when(videoRepository.findByIdWithTags(video.getId())).thenReturn(Optional.of(video));
        when(videoRepository.save(any(Video.class))).thenAnswer(inv -> inv.getArgument(0));
        when(videoMapper.toDto(any(Video.class))).thenReturn(new VideoDto());
    }

    private void update(MockMultipartFile thumbnail) {
        videoService.updateVideo(video.getId(), VideoUpdateDto.builder().title("t").status("PUBLIC").build(), thumbnail, userId);
    }

    @Test
    void replacingTheThumbnailDeletesTheOldOne() {
        when(cloudinaryService.uploadThumbnail(any(), anyBoolean())).thenReturn(NEW);

        update(file);

        verify(cloudinaryService).deleteImage(OLD);
    }

    @Test
    void noNewFileKeepsTheOldThumbnail() {
        update(null);

        verify(cloudinaryService, never()).deleteImage(any());
    }

    @Test
    void failedUploadKeepsTheOldThumbnailAndDeletesNothing() {
        when(cloudinaryService.uploadThumbnail(any(), anyBoolean())).thenThrow(new RuntimeException("Cloudinary down"));

        update(file);

        verify(cloudinaryService, never()).deleteImage(any());
    }

    @Test
    void videoWithoutAnOldThumbnailDeletesNothing() {
        video.setThumbnailUrl(null);
        when(cloudinaryService.uploadThumbnail(any(), anyBoolean())).thenReturn(NEW);

        update(file);

        verify(cloudinaryService, never()).deleteImage(any());
    }
}
