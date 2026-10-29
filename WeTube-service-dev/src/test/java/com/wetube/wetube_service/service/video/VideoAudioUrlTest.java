package com.wetube.wetube_service.service.video;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.springframework.data.domain.Page;

import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.channel.Channel;
import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.repository.channel.SubscriptionRepository;
import com.wetube.wetube_service.repository.interaction.CommentRepository;
import com.wetube.wetube_service.mapper.interaction.CommentMapper;
import com.wetube.wetube_service.repository.video.TagRepository;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.service.CloudinaryService;
import com.wetube.wetube_service.service.interaction.LikeService;
import com.wetube.wetube_service.service.premium.PremiumUserService;
import com.wetube.wetube_service.service.video.impl.VideoServiceImpl;

/** Phát nền: trang xem chỉ trả bản MP3 (audioUrl) cho người xem đang có Premium. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VideoAudioUrlTest {

    private static final String VIDEO_URL = "https://res.cloudinary.com/demo/video/upload/q_auto,vc_h264/v12/abc.mp4";

    @Mock VideoRepository videoRepository;
    @Mock PremiumUserService premiumUserService;
    @Mock CloudinaryService cloudinaryService;
    @Mock AudioWarmer audioWarmer;
    @Mock TagRepository tagRepository;
    @Mock CommentRepository commentRepository;
    @Mock SubscriptionRepository subscriptionRepository;
    @Mock VideoSubtitleService videoSubtitleService;
    @Mock LikeService likeService;
    @Mock CommentMapper commentMapper;
    @Mock WatchProgressService watchProgressService;

    @InjectMocks VideoServiceImpl videoService;

    private final UUID videoId = UUID.randomUUID();
    private final UUID viewerId = UUID.randomUUID();
    private Video video;

    @BeforeEach
    void setUp() {
        AppUser owner = new AppUser();
        owner.setId(UUID.randomUUID());
        owner.setChannel(Channel.builder().id(UUID.randomUUID()).build());
        video = new Video();
        video.setId(videoId);
        video.setUser(owner);
        video.setVideosStatus(ActiveStatus.ACTIVE);
        video.setVideoUrl(VIDEO_URL);
        video.setTotalView(1);
        video.setCreatedAt(LocalDateTime.now());
        when(videoRepository.findById(videoId)).thenReturn(Optional.of(video));
        when(videoRepository.findByVideosStatusIn(any(), any())).thenReturn(Page.empty());
        when(cloudinaryService.audioUrlFor(VIDEO_URL)).thenReturn("https://res.cloudinary.com/demo/video/upload/v12/abc.mp3");
    }

    private String audioUrlFor(UUID userId) {
        return videoService.getDetailWithRecommend(videoId, userId, false).getDetail().getAudioUrl();
    }

    @Test
    void premiumViewerGetsTheMp3Link() {
        when(premiumUserService.premiumUntil(viewerId)).thenReturn(Optional.of(LocalDate.now().plusDays(3)));

        assertThat(audioUrlFor(viewerId)).isEqualTo("https://res.cloudinary.com/demo/video/upload/v12/abc.mp3");
        verify(audioWarmer).warm(videoId, "https://res.cloudinary.com/demo/video/upload/v12/abc.mp3");
    }

    @Test
    void signedInViewerWithoutPremiumGetsNone() {
        when(premiumUserService.premiumUntil(viewerId)).thenReturn(Optional.empty());

        assertThat(audioUrlFor(viewerId)).isNull();
        verify(cloudinaryService, never()).audioUrlFor(any());
        verify(audioWarmer, never()).warm(any(), any());
    }

    @Test
    void guestGetsNone() {
        assertThat(audioUrlFor(null)).isNull();
        verify(premiumUserService, never()).premiumUntil(any());
    }

    @Test
    void aVideoNotOnCloudinaryGetsNoneEvenForPremium() {
        video.setVideoUrl("https://example.com/a.mp4");
        when(premiumUserService.premiumUntil(viewerId)).thenReturn(Optional.of(LocalDate.now().plusDays(3)));

        assertThat(audioUrlFor(viewerId)).isNull();
    }
}
