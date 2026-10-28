package com.wetube.wetube_service.service.video;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.channel.Channel;
import com.wetube.wetube_service.entity.video.Tag;
import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.mapper.video.VideoMapper;
import com.wetube.wetube_service.repository.UserRepository;
import com.wetube.wetube_service.repository.video.TagRepository;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.repository.video.VideoTagRepository;
import com.wetube.wetube_service.service.VideoIndexer;
import com.wetube.wetube_service.service.notification.NotificationService;
import com.wetube.wetube_service.service.video.VideoService.LiveReplay;
import com.wetube.wetube_service.service.video.impl.VideoServiceImpl;

/** Bản ghi buổi phát thành video công khai của kênh (L2). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LiveReplayTest {

    @Mock VideoRepository videoRepository;
    @Mock UserRepository userRepository;
    @Mock VideoIndexer videoIndexer;
    @Mock NotificationService notificationService;
    @Mock TagRepository tagRepository;
    @Mock VideoTagRepository videoTagRepository;
    @Mock VideoMapper videoMapper;

    @InjectMocks VideoServiceImpl videoService;

    private final UUID ownerId = UUID.randomUUID();
    private AppUser owner;
    private Video lastSaved;

    private LiveReplay replay() {
        return new LiveReplay(ownerId, "Buổi phát #hay", "Đã phát trực tiếp ngày 15/10/2026", "music",
                "https://pub.example/live/abc.mp4", "https://res.cloudinary.com/x/image/upload/t.jpg", 612f);
    }

    @BeforeEach
    void setUp() {
        owner = new AppUser();
        owner.setId(ownerId);
        owner.setChannel(Channel.builder().id(UUID.randomUUID()).status(ActiveStatus.ACTIVE).user(owner).build());
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(videoRepository.findFirstByVideoUrl(any())).thenReturn(Optional.empty());
        when(videoRepository.save(any(Video.class))).thenAnswer(inv -> {
            Video v = inv.getArgument(0);
            v.setId(UUID.randomUUID());
            lastSaved = v;
            return v;
        });
        // Tiêu đề có #hay nên video được gắn thẻ: addTags đọc lại video và lưu thẻ
        when(videoRepository.findById(any())).thenAnswer(inv -> Optional.ofNullable(lastSaved));
        when(videoRepository.findByIdWithTags(any())).thenAnswer(inv -> Optional.ofNullable(lastSaved));
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void theReplayIsAPrivateVideoOfTheOwnerWithTheRecordedFile() {
        UUID id = videoService.createLiveReplay(replay());

        ArgumentCaptor<Video> saved = ArgumentCaptor.forClass(Video.class);
        verify(videoRepository).save(saved.capture());
        Video v = saved.getValue();
        assertThat(id).isEqualTo(v.getId());
        assertThat(v.getUser()).isSameAs(owner);
        assertThat(v.getTitle()).isEqualTo("Buổi phát #hay");
        assertThat(v.getDescription()).startsWith("Đã phát trực tiếp ngày 15/10/2026");
        assertThat(v.getTopicId()).isEqualTo("music");
        assertThat(v.getVideoUrl()).isEqualTo("https://pub.example/live/abc.mp4");
        assertThat(v.getThumbnailUrl()).isEqualTo("https://res.cloudinary.com/x/image/upload/t.jpg");
        // riêng tư tới khi chủ kênh tự đổi chế độ hiển thị
        assertThat(v.getVideosStatus()).isEqualTo(ActiveStatus.PRIVATE);
        assertThat(v.getDuration()).isEqualTo(612f);
        assertThat(v.isShort()).isFalse();
        assertThat(v.getCreatedAt()).isNotNull();
    }

    @Test
    void theReplayIsHandedToTheIndexerButDoesNotNotifySubscribers() {
        videoService.createLiveReplay(replay());

        verify(videoIndexer).index(any(Video.class));
        verify(notificationService, never()).notifyNewVideo(any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void aReplayWithoutHashtagsIsIndexedExactlyOnceToo() {
        videoService.createLiveReplay(new LiveReplay(ownerId, "Buổi phát", "Không có thẻ", "music",
                "https://pub.example/live/def.mp4", null, 60f));

        verify(videoIndexer).index(any(Video.class));
    }

    @Test
    void creatingTheSameReplayAgainGivesBackTheExistingVideo() {
        Video existing = new Video();
        existing.setId(UUID.randomUUID());
        when(videoRepository.findFirstByVideoUrl("https://pub.example/live/abc.mp4")).thenReturn(Optional.of(existing));

        assertThat(videoService.createLiveReplay(replay())).isEqualTo(existing.getId());
        verify(videoRepository, never()).save(any(Video.class));
    }

    @Test
    void aHiddenChannelCannotPublishTheReplay() {
        owner.getChannel().setStatus(ActiveStatus.INACTIVE);

        assertThatThrownBy(() -> videoService.createLiveReplay(replay()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(videoRepository, never()).save(any(Video.class));
    }
}
