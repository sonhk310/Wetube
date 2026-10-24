package com.wetube.wetube_service.service.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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

import com.wetube.wetube_service.dto.response.analytics.ChannelAnalyticsDto;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.channel.Channel;
import com.wetube.wetube_service.repository.UserRepository;
import com.wetube.wetube_service.repository.channel.SubscriptionRepository;
import com.wetube.wetube_service.repository.interaction.CommentRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudioAnalyticsServiceTest {

    @Mock VideoStatsStore store;
    @Mock SubscriptionRepository subscriptionRepository;
    @Mock CommentRepository commentRepository;
    @Mock UserRepository userRepository;

    @InjectMocks StudioAnalyticsService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID channelId = UUID.randomUUID();
    private final LocalDate today = AnalyticsTime.today();
    private AppUser user;

    @BeforeEach
    void setUp() {
        user = new AppUser();
        user.setId(userId);
        Channel channel = new Channel();
        channel.setId(channelId);
        user.setChannel(channel);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(store.dailyTotals(eq(userId), any(), any())).thenReturn(List.of());
        when(store.firstDay(userId)).thenReturn(Optional.empty());
        when(subscriptionRepository.createdAtBetween(eq(channelId), any(), any())).thenReturn(List.of());
        when(commentRepository.videoCommentTimes(eq(userId), any(), any())).thenReturn(List.of());
    }

    @Test
    void rejectsOtherRanges() {
        assertThatThrownBy(() -> service.get(userId, 30)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fillsEveryDayOldestFirst() {
        ChannelAnalyticsDto dto = service.get(userId, 7);

        assertThat(dto.daily()).hasSize(7);
        assertThat(dto.daily().get(0).date()).isEqualTo(today.minusDays(6).toString());
        assertThat(dto.daily().get(6).date()).isEqualTo(today.toString());
        assertThat(dto.daily().get(3).views()).isZero();
        assertThat(dto.from()).isEqualTo(today.minusDays(6).toString());
        assertThat(dto.to()).isEqualTo(today.toString());
    }

    @Test
    void bucketsByVietnamDay() {
        // 23:30 UTC hôm qua = 06:30 sáng nay giờ VN
        LocalDateTime lateUtc = today.minusDays(1).atTime(23, 30);
        when(commentRepository.videoCommentTimes(eq(userId), any(), any())).thenReturn(List.of(lateUtc));

        ChannelAnalyticsDto dto = service.get(userId, 7);

        assertThat(dto.daily().get(6).comments()).isEqualTo(1);
        assertThat(dto.daily().get(5).comments()).isZero();
    }

    @Test
    void sumsTotals() {
        when(store.dailyTotals(eq(userId), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[] {Date.valueOf(today), 3L, 120L}));
        LocalDateTime now = AnalyticsTime.startOfDayUtc(today).plusHours(1);
        when(subscriptionRepository.createdAtBetween(eq(channelId), any(), any())).thenReturn(List.of(now, now));
        when(commentRepository.videoCommentTimes(eq(userId), any(), any())).thenReturn(List.of(now));

        ChannelAnalyticsDto dto = service.get(userId, 28);

        assertThat(dto.totals()).isEqualTo(new ChannelAnalyticsDto.Totals(3, 120, 2, 1));
        assertThat(dto.daily().get(27).views()).isEqualTo(3);
        assertThat(dto.daily().get(27).watchSeconds()).isEqualTo(120);
    }

    @Test
    void trackingSinceIsFirstStatsDayOrToday() {
        assertThat(service.get(userId, 7).trackingSince()).isEqualTo(today.toString());

        when(store.firstDay(userId)).thenReturn(Optional.of(today.minusDays(3)));
        assertThat(service.get(userId, 7).trackingSince()).isEqualTo(today.minusDays(3).toString());
    }

    @Test
    void userWithoutChannelGetsZeros() {
        user.setChannel(null);

        ChannelAnalyticsDto dto = service.get(userId, 7);

        assertThat(dto.totals()).isEqualTo(new ChannelAnalyticsDto.Totals(0, 0, 0, 0));
        verify(subscriptionRepository, never()).createdAtBetween(any(), any(), any());
    }
}
