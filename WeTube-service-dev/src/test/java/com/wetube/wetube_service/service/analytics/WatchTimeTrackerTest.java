package com.wetube.wetube_service.service.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.wetube.wetube_service.service.RedisService;

@ExtendWith(MockitoExtension.class)
class WatchTimeTrackerTest {

    @Mock RedisService redis;
    @Mock VideoStatsStore store;

    @InjectMocks WatchTimeTracker tracker;

    private final UUID videoId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();

    @Test
    void deltaCountsNormalAndFastPlaybackOnly() {
        assertThat(WatchTimeTracker.deltaSeconds(100f, 115f)).isEqualTo(15);
        assertThat(WatchTimeTracker.deltaSeconds(100f, 130f)).isEqualTo(30); // 2x
        assertThat(WatchTimeTracker.deltaSeconds(100f, 136f)).isZero();      // tua tới
        assertThat(WatchTimeTracker.deltaSeconds(100f, 40f)).isZero();       // tua lui / lặp lại
        assertThat(WatchTimeTracker.deltaSeconds(100f, 100f)).isZero();
        assertThat(WatchTimeTracker.deltaSeconds(null, 3f)).isEqualTo(3);
        assertThat(WatchTimeTracker.deltaSeconds(null, 500f)).isEqualTo(35);
    }

    @Test
    void recordAddsDeltaAndRemembersPosition() {
        when(redis.isAvailable()).thenReturn(true);
        when(redis.get("wt:u:x:" + videoId)).thenReturn("100.0");

        tracker.record("u:x", videoId, ownerId, 115f);

        verify(store).add(videoId, ownerId, AnalyticsTime.today(), 0, 15L);
        verify(redis).set("wt:u:x:" + videoId, "115.0", Duration.ofDays(1));
    }

    @Test
    void zeroDeltaWritesNothing() {
        when(redis.isAvailable()).thenReturn(true);
        when(redis.get("wt:u:x:" + videoId)).thenReturn("100.0");

        tracker.record("u:x", videoId, ownerId, 100f);

        verify(store, never()).add(any(), any(), any(), anyInt(), anyLong());
    }

    @Test
    void redisDownCountsNothing() {
        when(redis.isAvailable()).thenReturn(false);

        assertThatCode(() -> tracker.record("u:x", videoId, ownerId, 115f)).doesNotThrowAnyException();

        verify(store, never()).add(any(), any(), any(), anyInt(), anyLong());
    }
}
