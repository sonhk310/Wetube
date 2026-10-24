package com.wetube.wetube_service.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.service.video.VideoService;

@ExtendWith(MockitoExtension.class)
class DraftCleanupJobTest {

    @Mock VideoRepository videoRepository;
    @Mock VideoService videoService;

    @InjectMocks DraftCleanupJob job;

    private static Video draft() {
        Video v = new Video();
        v.setId(UUID.randomUUID());
        return v;
    }

    @Test
    void deletesDraftsUntouchedFor30Days() {
        Video a = draft();
        Video b = draft();
        when(videoRepository.findByVideosStatusAndUpdatedAtBefore(eq(ActiveStatus.DRAFT), any()))
                .thenReturn(List.of(a, b));

        job.deleteAbandonedDrafts();

        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(videoRepository).findByVideosStatusAndUpdatedAtBefore(eq(ActiveStatus.DRAFT), cutoff.capture());
        assertThat(cutoff.getValue()).isBefore(LocalDateTime.now().minusDays(29))
                .isAfter(LocalDateTime.now().minusDays(31));
        verify(videoService).deleteVideo(a.getId());
        verify(videoService).deleteVideo(b.getId());
    }

    @Test
    void oneFailureDoesNotStopTheRest() {
        Video a = draft();
        Video b = draft();
        when(videoRepository.findByVideosStatusAndUpdatedAtBefore(eq(ActiveStatus.DRAFT), any()))
                .thenReturn(List.of(a, b));
        doThrow(new RuntimeException("boom")).when(videoService).deleteVideo(a.getId());

        job.deleteAbandonedDrafts();

        verify(videoService).deleteVideo(b.getId());
    }
}
