package com.wetube.wetube_service.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import com.wetube.wetube_service.dto.video.RecommendVideoDto;
import com.wetube.wetube_service.service.video.RecService;

/** Gợi ý phản ánh sở thích riêng: chỉ chủ tài khoản được đọc. */
class RecommendationControllerTest {

    private final RecService recService = mock(RecService.class);
    private final RecommendationController controller = new RecommendationController(recService);

    private static Jwt jwtOf(UUID userId) {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(userId.toString());
        return jwt;
    }

    @Test
    void ownerReadsOwnRecommendations() {
        UUID me = UUID.randomUUID();
        PageRequest page = PageRequest.of(0, 10);
        when(recService.recommendVideos(me, page)).thenReturn(Page.<RecommendVideoDto>empty(page));

        assertThat(controller.recommendVideos(me, page, jwtOf(me)).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void otherUserIsForbidden() {
        UUID victim = UUID.randomUUID();

        assertThatThrownBy(() -> controller.recommendVideos(victim, PageRequest.of(0, 10), jwtOf(UUID.randomUUID())))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> controller.getTopRankedVideos(victim, 100, 5, jwtOf(UUID.randomUUID())))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(recService, never()).recommendVideos(any(), any());
        verify(recService, never()).findTopRankedVideos(any(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void anonymousIsUnauthorized() {
        UUID victim = UUID.randomUUID();

        assertThatThrownBy(() -> controller.recommendVideos(victim, PageRequest.of(0, 10), null))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }
}
