package com.wetube.wetube_service.service.video;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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

import com.wetube.wetube_service.dto.video.VideoDto;
import com.wetube.wetube_service.dto.video.VideoUpdateDto;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.mapper.video.VideoMapper;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.repository.video.VideoTagRepository;
import com.wetube.wetube_service.service.VideoIndexer;
import com.wetube.wetube_service.service.notification.NotificationService;
import com.wetube.wetube_service.service.video.impl.VideoServiceImpl;

/** Ngày đăng của bản nháp là lúc đăng, không phải lúc tải lên. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VideoPublishTest {

    @Mock VideoRepository videoRepository;
    @Mock VideoTagRepository videoTagRepository;
    @Mock VideoIndexer videoIndexer;
    @Mock NotificationService notificationService;
    @Mock VideoMapper videoMapper;

    @InjectMocks VideoServiceImpl videoService;

    private final UUID userId = UUID.randomUUID();
    private final LocalDateTime uploadedAt = LocalDateTime.now().minusDays(3);
    private Video video;

    @BeforeEach
    void setUp() {
        AppUser user = new AppUser();
        user.setId(userId);
        video = new Video();
        video.setId(UUID.randomUUID());
        video.setUser(user);
        video.setCreatedAt(uploadedAt);
        when(videoRepository.findById(video.getId())).thenReturn(Optional.of(video));
        when(videoRepository.findByIdWithTags(video.getId())).thenReturn(Optional.of(video));
        when(videoRepository.save(any(Video.class))).thenAnswer(inv -> inv.getArgument(0));
        when(videoMapper.toDto(any(Video.class))).thenReturn(new VideoDto());
    }

    private void update(String status) {
        videoService.updateVideo(video.getId(),
                VideoUpdateDto.builder().title("t").status(status).build(), null, userId);
    }

    @Test
    void publishingDraftSetsDateToNow() {
        video.setVideosStatus(ActiveStatus.DRAFT);

        update("PUBLIC");

        assertThat(video.getCreatedAt()).isAfter(LocalDateTime.now().minusMinutes(1));
    }

    @Test
    void savingDraftAsPrivateAlsoCountsAsPublishing() {
        video.setVideosStatus(ActiveStatus.DRAFT);

        update("PRIVATE");

        assertThat(video.getCreatedAt()).isAfter(LocalDateTime.now().minusMinutes(1));
    }

    @Test
    void savingDraftAsDraftKeepsDraftAndDate() {
        video.setVideosStatus(ActiveStatus.DRAFT);

        update("DRAFT");

        assertThat(video.getVideosStatus()).isEqualTo(ActiveStatus.DRAFT);
        assertThat(video.getCreatedAt()).isEqualTo(uploadedAt);
    }

    @Test
    void publishedVideoCannotGoBackToDraft() {
        video.setVideosStatus(ActiveStatus.PRIVATE);

        assertThatThrownBy(() -> update("DRAFT")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void editingPublishedVideoKeepsDate() {
        video.setVideosStatus(ActiveStatus.PRIVATE);

        update("PUBLIC");

        assertThat(video.getCreatedAt()).isEqualTo(uploadedAt);
    }
}
