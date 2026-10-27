package com.wetube.wetube_service.service.video;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.wetube.wetube_service.dto.video.VideoDto;
import com.wetube.wetube_service.dto.video.VideoUpdateDto;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.channel.Channel;
import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.mapper.video.VideoMapper;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.repository.video.VideoTagRepository;
import com.wetube.wetube_service.service.VideoIndexer;
import com.wetube.wetube_service.service.notification.NotificationService;
import com.wetube.wetube_service.service.video.impl.VideoServiceImpl;

/** C8: chủ đề bắt buộc khi đăng / lưu video đã đăng, không bắt buộc khi vẫn để là bản nháp. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VideoTopicUpdateTest {

    @Mock VideoRepository videoRepository;
    @Mock VideoIndexer videoIndexer;
    @Mock NotificationService notificationService;
    @Mock VideoMapper videoMapper;
    @Mock VideoTagRepository videoTagRepository;
    @Spy TopicCatalog topicCatalog = new TopicCatalog();

    @InjectMocks VideoServiceImpl videoService;

    private final UUID userId = UUID.randomUUID();
    private Video video;

    @BeforeEach
    void setUp() {
        AppUser user = new AppUser();
        user.setId(userId);
        user.setChannel(Channel.builder().id(UUID.randomUUID()).status(ActiveStatus.ACTIVE).user(user).build());
        video = new Video();
        video.setId(UUID.randomUUID());
        video.setUser(user);
        video.setVideosStatus(ActiveStatus.DRAFT);
        when(videoRepository.findById(video.getId())).thenReturn(Optional.of(video));
        when(videoRepository.findByIdWithTags(video.getId())).thenReturn(Optional.of(video));
        when(videoRepository.save(any(Video.class))).thenAnswer(inv -> inv.getArgument(0));
        when(videoMapper.toDto(any(Video.class))).thenReturn(new VideoDto());
    }

    private void update(String status, String topicId) {
        videoService.updateVideo(video.getId(),
                VideoUpdateDto.builder().title("t").status(status).topicId(topicId).build(), null, userId);
    }

    @Test
    void publishingWithoutATopicIsRejected() {
        assertThatThrownBy(() -> update("PUBLIC", null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("chủ đề");
        assertThat(video.getVideosStatus()).isEqualTo(ActiveStatus.DRAFT);
    }

    @Test
    void anUnknownTopicIsRejectedEvenForADraft() {
        assertThatThrownBy(() -> update("DRAFT", "music.khong-co"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aDraftCanBeSavedWithoutATopic() {
        update("DRAFT", null);

        assertThat(video.getTopicId()).isNull();
        assertThat(video.getVideosStatus()).isEqualTo(ActiveStatus.DRAFT);
    }

    @Test
    void publishingWithASubTopicOrAWholeGroupIsSaved() {
        update("PUBLIC", " music.lo-fi ");
        assertThat(video.getTopicId()).isEqualTo("music.lo-fi");

        update("PRIVATE", "sports");
        assertThat(video.getTopicId()).isEqualTo("sports");
    }

    @Test
    void notSendingATopicKeepsTheOneAlreadySaved() {
        video.setTopicId("music");
        video.setVideosStatus(ActiveStatus.PUBLIC);

        update("PUBLIC", "");

        assertThat(video.getTopicId()).isEqualTo("music");
    }

    @Test
    void anOldPublishedVideoWithoutATopicMustGetOneBeforeSaving() {
        video.setVideosStatus(ActiveStatus.PUBLIC);

        assertThatThrownBy(() -> update("PUBLIC", null)).isInstanceOf(IllegalArgumentException.class);
    }
}
