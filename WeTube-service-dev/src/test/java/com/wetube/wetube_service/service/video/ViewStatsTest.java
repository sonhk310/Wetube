package com.wetube.wetube_service.service.video;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
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

import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.service.RedisService;
import com.wetube.wetube_service.service.VideoIndexer;
import com.wetube.wetube_service.service.analytics.AnalyticsTime;
import com.wetube.wetube_service.service.analytics.VideoStatsStore;
import com.wetube.wetube_service.service.video.impl.VideoServiceImpl;

/** Lượt xem được đếm thì cũng cộng vào số liệu theo ngày của chủ video. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ViewStatsTest {

    @Mock VideoRepository videoRepository;
    @Mock RedisService redisService;
    @Mock VideoIndexer videoIndexer;
    @Mock VideoStatsStore videoStatsStore;

    @InjectMocks VideoServiceImpl videoService;

    private final UUID videoId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        AppUser owner = new AppUser();
        owner.setId(ownerId);
        Video video = new Video();
        video.setId(videoId);
        video.setUser(owner);
        video.setVideosStatus(ActiveStatus.ACTIVE);
        video.setTotalView(5);
        when(videoRepository.findById(videoId)).thenReturn(Optional.of(video));
        when(videoRepository.findTotalViewById(videoId)).thenReturn(Optional.of(6));
    }

    @Test
    void countedViewAddsOneViewForOwnerToday() {
        when(redisService.setIfAbsent(anyString(), any())).thenReturn(true);

        videoService.registerView(videoId, "a:x", null, false);

        verify(videoStatsStore).add(videoId, ownerId, AnalyticsTime.today(), 1, 0L);
    }

    @Test
    void repeatedViewAddsNothing() {
        when(redisService.setIfAbsent(anyString(), any())).thenReturn(false);

        videoService.registerView(videoId, "a:x", null, false);

        verify(videoStatsStore, never()).add(any(), any(), any(), anyInt(), anyLong());
    }

    @Test
    void statsFailureDoesNotBreakView() {
        when(redisService.setIfAbsent(anyString(), any())).thenReturn(true);
        doThrow(new RuntimeException("bảng chưa có")).when(videoStatsStore)
                .add(any(), any(), any(), anyInt(), anyLong());

        Map<String, Object> result = videoService.registerView(videoId, "a:x", null, false);

        assertThat(result).containsEntry("counted", true).containsEntry("totalView", 6);
    }
}
