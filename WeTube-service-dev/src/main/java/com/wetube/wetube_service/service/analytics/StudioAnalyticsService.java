package com.wetube.wetube_service.service.analytics;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wetube.wetube_service.dto.response.analytics.ChannelAnalyticsDto;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.channel.Channel;
import com.wetube.wetube_service.repository.UserRepository;
import com.wetube.wetube_service.repository.channel.SubscriptionRepository;
import com.wetube.wetube_service.repository.interaction.CommentRepository;

import lombok.RequiredArgsConstructor;

/** Số liệu phân tích của kênh theo ngày (giờ VN) cho trang Studio. */
@Service
@RequiredArgsConstructor
public class StudioAnalyticsService {
    private static final Set<Integer> RANGES = Set.of(7, 28, 90);

    private final VideoStatsStore store;
    private final SubscriptionRepository subscriptionRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ChannelAnalyticsDto get(UUID userId, int days) {
        if (!RANGES.contains(days)) {
            throw new IllegalArgumentException("Khoảng thời gian chỉ được là 7, 28 hoặc 90 ngày");
        }
        LocalDate to = AnalyticsTime.today();
        LocalDate from = to.minusDays(days - 1L);
        LocalDateTime fromUtc = AnalyticsTime.startOfDayUtc(from);
        LocalDateTime toUtc = AnalyticsTime.startOfDayUtc(to.plusDays(1));

        long[] views = new long[days];
        long[] watch = new long[days];
        long[] subs = new long[days];
        long[] comments = new long[days];

        for (Object[] row : store.dailyTotals(userId, from, to)) {
            int i = index(from, toLocalDate(row[0]), days);
            if (i >= 0) {
                views[i] += ((Number) row[1]).longValue();
                watch[i] += ((Number) row[2]).longValue();
            }
        }
        Channel channel = userRepository.findById(userId).map(AppUser::getChannel).orElse(null);
        if (channel != null) {
            count(subscriptionRepository.createdAtBetween(channel.getId(), fromUtc, toUtc), from, subs);
            count(commentRepository.videoCommentTimes(userId, fromUtc, toUtc), from, comments);
        }

        List<ChannelAnalyticsDto.Day> daily = new ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            daily.add(new ChannelAnalyticsDto.Day(from.plusDays(i).toString(), views[i], watch[i], subs[i], comments[i]));
        }
        ChannelAnalyticsDto.Totals totals = new ChannelAnalyticsDto.Totals(
                sum(views), sum(watch), sum(subs), sum(comments));
        String trackingSince = store.firstDay(userId).orElse(to).toString();
        return new ChannelAnalyticsDto(from.toString(), to.toString(), trackingSince, totals, daily);
    }

    private static void count(List<LocalDateTime> times, LocalDate from, long[] buckets) {
        for (LocalDateTime t : times) {
            int i = index(from, AnalyticsTime.dayOf(t), buckets.length);
            if (i >= 0) {
                buckets[i]++;
            }
        }
    }

    private static int index(LocalDate from, LocalDate day, int days) {
        long i = day.toEpochDay() - from.toEpochDay();
        return (i >= 0 && i < days) ? (int) i : -1;
    }

    private static LocalDate toLocalDate(Object value) {
        return value instanceof Date d ? d.toLocalDate() : (LocalDate) value;
    }

    private static long sum(long[] values) {
        long total = 0;
        for (long v : values) {
            total += v;
        }
        return total;
    }
}
