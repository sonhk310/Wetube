package com.wetube.wetube_service.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.security.OwnershipGuard;
import com.wetube.wetube_service.security.RateLimiter;
import com.wetube.wetube_service.service.AI.SubtitleAiService;
import com.wetube.wetube_service.service.video.VideoSubtitleService;

/** Phụ đề AI chỉ cho video tối đa 10 phút; quá giới hạn của người dùng hoặc của cả trang thì 429. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubtitleAiLimitTest {

    @Mock VideoSubtitleService subtitleService;
    @Mock SubtitleAiService subtitleAiService;
    @Mock VideoRepository videoRepository;
    @Mock OwnershipGuard ownershipGuard;
    @Mock RateLimiter rateLimiter;

    @InjectMocks VideoSubtitleController controller;

    private final UUID videoId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final Video video = new Video();
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        video.setId(videoId);
        video.setVideoUrl("https://cdn/v.mp4");
        when(videoRepository.findById(videoId)).thenReturn(Optional.of(video));
        jwt = Jwt.withTokenValue("t").header("alg", "none").subject(userId.toString())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
    }

    @Test
    void videoLongerThanTenMinutesIsRefusedWithoutUsingQuota() {
        video.setDuration(601);

        assertThatThrownBy(() -> controller.generateAiSubtitle(videoId, "vi", jwt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10 phút");
        verifyNoInteractions(rateLimiter, subtitleAiService);
    }

    @Test
    void tenMinutesExactlyAndUnknownDurationAreAllowed() {
        video.setDuration(600);
        controller.generateAiSubtitle(videoId, "vi", jwt);
        video.setDuration(0);
        controller.generateAiSubtitle(videoId, "vi", jwt);

        verify(rateLimiter, org.mockito.Mockito.times(2)).check(eq(RateLimiter.Limit.AI_SUBTITLE), eq(userId));
        verify(rateLimiter, org.mockito.Mockito.times(2)).check(eq(RateLimiter.Limit.AI_SUBTITLE_DAILY), any(LocalDate.class));
        verify(subtitleAiService, org.mockito.Mockito.times(2)).start(videoId, "https://cdn/v.mp4", "vi");
    }

    @Test
    void siteWideDailyLimitStopsGeneration() {
        video.setDuration(120);
        org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "hết lượt"))
                .when(rateLimiter).check(eq(RateLimiter.Limit.AI_SUBTITLE_DAILY), any());

        assertThatThrownBy(() -> controller.generateAiSubtitle(videoId, "vi", jwt))
                .isInstanceOf(ResponseStatusException.class);
        verify(subtitleAiService, never()).start(any(), any(), any());
    }
}
