package com.wetube.wetube_service.service.video;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.security.RateLimiter;
import com.wetube.wetube_service.security.VideoAccess;
import com.wetube.wetube_service.service.premium.PremiumUserService;

/** Tải video về máy: chỉ Premium, chỉ video xem được, 30 lần mỗi giờ, trả link Cloudinary có flag tải về. */
class VideoDownloadServiceTest {

    private static final String VIDEO_URL = "https://res.cloudinary.com/demo/video/upload/q_auto,vc_h264/v12/abc.mp4";

    private final PremiumUserService premium = mock(PremiumUserService.class);
    private final VideoAccess access = mock(VideoAccess.class);
    private final VideoRepository videos = mock(VideoRepository.class);
    private final RateLimiter limiter = mock(RateLimiter.class);
    private final VideoDownloadService service = new VideoDownloadService(premium, access, videos, limiter);

    private final UUID userId = UUID.randomUUID();
    private final UUID videoId = UUID.randomUUID();
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        jwt = Jwt.withTokenValue("t").header("alg", "none").subject(userId.toString()).claim("roles", List.of()).build();
        when(premium.premiumUntil(userId)).thenReturn(Optional.of(LocalDate.now().plusDays(5)));
        Video video = Video.builder().id(videoId).title("Bài hát Đẹp nhất!").videoUrl(VIDEO_URL).build();
        when(videos.findById(videoId)).thenReturn(Optional.of(video));
    }

    private static void assertStatus(Throwable t, HttpStatus expected) {
        assertThat(t).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) t).getStatusCode()).isEqualTo(expected);
    }

    @Test
    void premiumGetsTheCloudinaryAttachmentLink() {
        String url = service.downloadUrl(videoId, jwt);

        assertThat(url).isEqualTo("https://res.cloudinary.com/demo/video/upload/fl_attachment:Bai_hat_Dep_nhat/q_auto,vc_h264/v12/abc.mp4");
        verify(limiter).check(RateLimiter.Limit.DOWNLOAD, userId);
    }

    @Test
    void notLoggedInIs401() {
        assertThatThrownBy(() -> service.downloadUrl(videoId, null)).satisfies(t -> assertStatus(t, HttpStatus.UNAUTHORIZED));
    }

    @Test
    void withoutPremiumIs403AndNothingElseIsTouched() {
        when(premium.premiumUntil(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.downloadUrl(videoId, jwt)).satisfies(t -> {
            assertStatus(t, HttpStatus.FORBIDDEN);
            assertThat(((ResponseStatusException) t).getReason()).contains("Premium");
        });
        verify(access, never()).requireViewable(any(), any());
        verify(limiter, never()).check(any(), any());
    }

    @Test
    void aVideoTheCallerCannotViewIs404AndNotCounted() {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy video"))
                .when(access).requireViewable(videoId, jwt);

        assertThatThrownBy(() -> service.downloadUrl(videoId, jwt)).satisfies(t -> assertStatus(t, HttpStatus.NOT_FOUND));
        verify(limiter, never()).check(any(), any());
    }

    @Test
    void aDeletedVideoRowIs404() {
        when(videos.findById(videoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.downloadUrl(videoId, jwt)).satisfies(t -> assertStatus(t, HttpStatus.NOT_FOUND));
    }

    @Test
    void aVideoWithoutAFileIs404() {
        when(videos.findById(videoId)).thenReturn(Optional.of(Video.builder().id(videoId).title("x").videoUrl(" ").build()));

        assertThatThrownBy(() -> service.downloadUrl(videoId, jwt)).satisfies(t -> assertStatus(t, HttpStatus.NOT_FOUND));
    }

    @Test
    void overTheHourlyLimitIs429() {
        doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "quá nhiều"))
                .when(limiter).check(eq(RateLimiter.Limit.DOWNLOAD), eq(userId));

        assertThatThrownBy(() -> service.downloadUrl(videoId, jwt)).satisfies(t -> assertStatus(t, HttpStatus.TOO_MANY_REQUESTS));
    }
}
