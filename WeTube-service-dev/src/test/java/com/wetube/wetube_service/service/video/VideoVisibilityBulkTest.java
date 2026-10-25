package com.wetube.wetube_service.service.video;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.service.VideoIndexer;
import com.wetube.wetube_service.service.notification.NotificationService;
import com.wetube.wetube_service.service.video.impl.VideoServiceImpl;

/** Đổi chế độ hiển thị nhiều video một lần (Studio, chọn nhiều hàng). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VideoVisibilityBulkTest {

    @Mock VideoRepository videoRepository;
    @Mock VideoIndexer videoIndexer;
    @Mock NotificationService notificationService;

    @InjectMocks VideoServiceImpl videoService;

    private final UUID ownerId = UUID.randomUUID();
    private AppUser owner;

    @BeforeEach
    void setUp() {
        owner = new AppUser();
        owner.setId(ownerId);
        when(videoRepository.save(any(Video.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Video video(ActiveStatus status, AppUser user) {
        Video v = new Video();
        v.setId(UUID.randomUUID());
        v.setUser(user);
        v.setVideosStatus(status);
        when(videoRepository.findByIdWithTags(v.getId())).thenReturn(Optional.of(v));
        return v;
    }

    @Test
    void changesPublishedVideosAndSkipsDraftsHiddenAndUnchanged() {
        Video priv = video(ActiveStatus.PRIVATE, owner);
        Video unlisted = video(ActiveStatus.UNLISTED, owner);
        Video draft = video(ActiveStatus.DRAFT, owner);
        Video hidden = video(ActiveStatus.INACTIVE, owner);
        Video alreadyPublic = video(ActiveStatus.ACTIVE, owner);
        List<Video> all = List.of(priv, unlisted, draft, hidden, alreadyPublic);
        when(videoRepository.findAllById(any())).thenReturn(all);

        int updated = videoService.updateVisibility(all.stream().map(Video::getId).toList(), "PUBLIC", ownerId);

        assertThat(updated).isEqualTo(2);
        assertThat(priv.getVideosStatus()).isEqualTo(ActiveStatus.ACTIVE);
        assertThat(unlisted.getVideosStatus()).isEqualTo(ActiveStatus.ACTIVE);
        assertThat(draft.getVideosStatus()).isEqualTo(ActiveStatus.DRAFT);
        assertThat(hidden.getVideosStatus()).isEqualTo(ActiveStatus.INACTIVE);
        // Video dùng equals của Lombok (hai video cùng trạng thái bị coi là bằng nhau): so sánh đúng đối tượng
        ArgumentCaptor<Video> indexed = ArgumentCaptor.forClass(Video.class);
        verify(videoIndexer, times(2)).index(indexed.capture());
        assertThat(indexed.getAllValues()).allMatch(v -> v == priv || v == unlisted);
    }

    @Test
    void rejectsVideosOfOtherUsers() {
        AppUser stranger = new AppUser();
        stranger.setId(UUID.randomUUID());
        Video mine = video(ActiveStatus.PRIVATE, owner);
        Video theirs = video(ActiveStatus.PRIVATE, stranger);
        when(videoRepository.findAllById(any())).thenReturn(List.of(mine, theirs));

        assertThatThrownBy(() -> videoService.updateVisibility(List.of(mine.getId(), theirs.getId()), "PUBLIC", ownerId))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(mine.getVideosStatus()).isEqualTo(ActiveStatus.PRIVATE);
    }

    @Test
    void rejectsInvalidStatus() {
        assertThatThrownBy(() -> videoService.updateVisibility(List.of(UUID.randomUUID()), "DRAFT", ownerId))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
