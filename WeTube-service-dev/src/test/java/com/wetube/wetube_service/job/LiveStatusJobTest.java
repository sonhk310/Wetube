package com.wetube.wetube_service.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.wetube.wetube_service.service.live.LiveStreamService;

class LiveStatusJobTest {

    private final LiveStreamService service = mock(LiveStreamService.class);
    private final LiveStatusJob job = new LiveStatusJob(service);

    @Test
    void tickRefreshesWithTheCurrentTime() {
        LocalDateTime before = LocalDateTime.now();

        job.tick();

        ArgumentCaptor<LocalDateTime> now = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(service).refreshAll(now.capture());
        assertThat(now.getValue()).isBetween(before, LocalDateTime.now());
    }

    @Test
    void tickNeverThrowsEvenWhenTheServiceDoes() {
        doThrow(new IllegalStateException("db down")).when(service).refreshAll(any());

        assertThatCode(job::tick).doesNotThrowAnyException();
    }

    @Test
    void midnightJobCleansInactiveIngress() {
        job.cleanIngress();

        verify(service).cleanInactiveIngress();
    }
}
