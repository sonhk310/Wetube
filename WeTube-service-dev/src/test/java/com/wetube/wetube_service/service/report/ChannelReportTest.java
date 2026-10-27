package com.wetube.wetube_service.service.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.channel.Channel;
import com.wetube.wetube_service.entity.report.Report;
import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.repository.channel.ChannelRepository;
import com.wetube.wetube_service.repository.interaction.CommentRepository;
import com.wetube.wetube_service.repository.report.ReportRepository;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.service.VideoIndexer;
import com.wetube.wetube_service.service.interaction.CommentService;

/** "Báo cáo người dùng" ở hộp Giới thiệu của kênh và nút "Ẩn kênh" của admin. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChannelReportTest {

    @Mock ReportRepository reportRepository;
    @Mock VideoRepository videoRepository;
    @Mock VideoIndexer videoIndexer;
    @Mock CommentRepository commentRepository;
    @Mock CommentService commentService;
    @Mock ChannelRepository channelRepository;
    @InjectMocks ReportService service;

    private final UUID reporterId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();
    private Channel channel;

    @BeforeEach
    void setUp() {
        AppUser owner = AppUser.builder().id(ownerId).build();
        channel = Channel.builder().id(UUID.randomUUID()).name("Kênh A").handle("kenha")
                .status(ActiveStatus.ACTIVE).user(owner).build();
        when(channelRepository.findById(channel.getId())).thenReturn(Optional.of(channel));
        when(channelRepository.findAllById(anyIterable())).thenReturn(List.of(channel));
        when(videoRepository.findAllById(anyIterable())).thenReturn(List.of());
        when(reportRepository.save(any(Report.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void reportingAChannelStoresChannelIdWithoutVideo() {
        var view = service.create(reporterId, null, null, channel.getId(), "IMPERSONATION", "  giả mạo  ");

        ArgumentCaptor<Report> saved = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(saved.capture());
        assertThat(saved.getValue().getChannelId()).isEqualTo(channel.getId());
        assertThat(saved.getValue().getVideoId()).isNull();
        assertThat(saved.getValue().getDetails()).isEqualTo("giả mạo");
        assertThat(view.channelName()).isEqualTo("Kênh A");
        assertThat(view.channelHandle()).isEqualTo("kenha");
        assertThat(view.videoTitle()).isNull();
    }

    @Test
    void videoOnlyReasonIsRejectedForChannels() {
        assertThatThrownBy(() -> service.create(reporterId, null, null, channel.getId(), "COPYRIGHT", null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(reportRepository, never()).save(any());
    }

    @Test
    void cannotReportOwnChannel() {
        assertThatThrownBy(() -> service.create(ownerId, null, null, channel.getId(), "SPAM", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chính mình");
    }

    @Test
    void secondOpenReportOfSameChannelIsRejected() {
        when(reportRepository.existsByChannelIdAndReporterIdAndStatus(channel.getId(), reporterId, Report.Status.OPEN))
                .thenReturn(true);
        assertThatThrownBy(() -> service.create(reporterId, null, null, channel.getId(), "SPAM", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("đã báo cáo kênh");
    }

    @Test
    void hideChannelHidesChannelAndItsVisibleVideosAndClosesOpenReports() {
        Report report = Report.builder().id(UUID.randomUUID()).channelId(channel.getId())
                .reporterId(reporterId).reason("SPAM").status(Report.Status.OPEN).build();
        Report other = Report.builder().id(UUID.randomUUID()).channelId(channel.getId())
                .reporterId(UUID.randomUUID()).reason("HATE").status(Report.Status.OPEN).build();
        Video publicVideo = Video.builder().id(UUID.randomUUID()).videosStatus(ActiveStatus.PUBLIC).build();
        Video unlisted = Video.builder().id(UUID.randomUUID()).videosStatus(ActiveStatus.UNLISTED).build();
        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(reportRepository.findByChannelIdAndStatus(channel.getId(), Report.Status.OPEN))
                .thenReturn(List.of(report, other));
        when(videoRepository.findByUser_IdAndVideosStatusIn(ownerId,
                List.of(ActiveStatus.ACTIVE, ActiveStatus.PUBLIC, ActiveStatus.UNLISTED)))
                .thenReturn(List.of(publicVideo, unlisted));

        var view = service.resolve(report.getId(), ReportService.Action.HIDE_CHANNEL);

        assertThat(channel.getStatus()).isEqualTo(ActiveStatus.INACTIVE);
        assertThat(publicVideo.getVideosStatus()).isEqualTo(ActiveStatus.INACTIVE);
        assertThat(unlisted.getVideosStatus()).isEqualTo(ActiveStatus.INACTIVE);
        verify(videoIndexer).remove(publicVideo.getId());
        verify(videoIndexer).remove(unlisted.getId());
        assertThat(other.getStatus()).isEqualTo(Report.Status.RESOLVED);
        assertThat(view.status()).isEqualTo("RESOLVED");
        assertThat(view.resolutionNote()).isEqualTo("Kênh đã bị ẩn");
    }

    @Test
    void hideChannelRemembersEachVideosStatus() {
        Report report = Report.builder().id(UUID.randomUUID()).channelId(channel.getId())
                .reporterId(reporterId).reason("SPAM").status(Report.Status.OPEN).build();
        Video unlisted = Video.builder().id(UUID.randomUUID()).videosStatus(ActiveStatus.UNLISTED).build();
        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(videoRepository.findByUser_IdAndVideosStatusIn(any(), any())).thenReturn(List.of(unlisted));

        service.resolve(report.getId(), ReportService.Action.HIDE_CHANNEL);

        assertThat(unlisted.getStatusBeforeHidden()).isEqualTo(ActiveStatus.UNLISTED);
        assertThat(unlisted.getHiddenWithChannel()).isTrue();
    }

    @Test
    void unhideChannelRestoresOnlyVideosHiddenWithTheChannel() {
        channel.setStatus(ActiveStatus.INACTIVE);
        Report report = Report.builder().id(UUID.randomUUID()).channelId(channel.getId())
                .reporterId(reporterId).reason("SPAM").status(Report.Status.RESOLVED).build();
        Video withChannel = Video.builder().id(UUID.randomUUID()).videosStatus(ActiveStatus.INACTIVE)
                .statusBeforeHidden(ActiveStatus.UNLISTED).hiddenWithChannel(true).build();
        Video hiddenAlone = Video.builder().id(UUID.randomUUID()).videosStatus(ActiveStatus.INACTIVE)
                .statusBeforeHidden(ActiveStatus.PUBLIC).hiddenWithChannel(false).build();
        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(videoRepository.findByUser_IdAndVideosStatusIn(ownerId, List.of(ActiveStatus.INACTIVE)))
                .thenReturn(List.of(withChannel, hiddenAlone));

        var view = service.resolve(report.getId(), ReportService.Action.UNHIDE_CHANNEL);

        assertThat(channel.getStatus()).isEqualTo(ActiveStatus.ACTIVE);
        assertThat(withChannel.getVideosStatus()).isEqualTo(ActiveStatus.UNLISTED);
        assertThat(withChannel.getStatusBeforeHidden()).isNull();
        verify(videoIndexer).index(withChannel);
        assertThat(hiddenAlone.getVideosStatus()).isEqualTo(ActiveStatus.INACTIVE);
        assertThat(view.resolutionNote()).isEqualTo("Đã bỏ ẩn kênh");
    }

    @Test
    void hideThenUnhideVideoRestoresItsStatus() {
        Video video = Video.builder().id(UUID.randomUUID()).videosStatus(ActiveStatus.UNLISTED)
                .user(channel.getUser()).build();
        channel.getUser().setChannel(channel);
        Report report = Report.builder().id(UUID.randomUUID()).videoId(video.getId())
                .reporterId(reporterId).reason("SPAM").status(Report.Status.OPEN).build();
        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(videoRepository.findById(video.getId())).thenReturn(Optional.of(video));

        service.resolve(report.getId(), ReportService.Action.HIDE_VIDEO);
        assertThat(video.getVideosStatus()).isEqualTo(ActiveStatus.INACTIVE);
        assertThat(video.getStatusBeforeHidden()).isEqualTo(ActiveStatus.UNLISTED);

        service.resolve(report.getId(), ReportService.Action.UNHIDE_VIDEO);
        assertThat(video.getVideosStatus()).isEqualTo(ActiveStatus.UNLISTED);
        verify(videoIndexer).index(video);
    }

    @Test
    void videoHiddenBeforeThisChangeIsRestoredAsPublic() {
        Video video = Video.builder().id(UUID.randomUUID()).videosStatus(ActiveStatus.INACTIVE).build();
        Report report = Report.builder().id(UUID.randomUUID()).videoId(video.getId())
                .reporterId(reporterId).reason("SPAM").status(Report.Status.RESOLVED).build();
        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(videoRepository.findById(video.getId())).thenReturn(Optional.of(video));

        service.resolve(report.getId(), ReportService.Action.UNHIDE_VIDEO);

        assertThat(video.getVideosStatus()).isEqualTo(ActiveStatus.PUBLIC);
    }

    @Test
    void cannotUnhideVideoWhileItsChannelIsHidden() {
        channel.setStatus(ActiveStatus.INACTIVE);
        channel.getUser().setChannel(channel);
        Video video = Video.builder().id(UUID.randomUUID()).videosStatus(ActiveStatus.INACTIVE)
                .user(channel.getUser()).build();
        Report report = Report.builder().id(UUID.randomUUID()).videoId(video.getId())
                .reporterId(reporterId).reason("SPAM").status(Report.Status.RESOLVED).build();
        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(videoRepository.findById(video.getId())).thenReturn(Optional.of(video));

        assertThatThrownBy(() -> service.resolve(report.getId(), ReportService.Action.UNHIDE_VIDEO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bỏ ẩn kênh trước");
        assertThat(video.getVideosStatus()).isEqualTo(ActiveStatus.INACTIVE);
    }

    @Test
    void hideChannelOnAVideoReportIsRejected() {
        Report videoReport = Report.builder().id(UUID.randomUUID()).videoId(UUID.randomUUID())
                .reporterId(reporterId).reason("SPAM").status(Report.Status.OPEN).build();
        when(reportRepository.findById(videoReport.getId())).thenReturn(Optional.of(videoReport));

        assertThatThrownBy(() -> service.resolve(videoReport.getId(), ReportService.Action.HIDE_CHANNEL))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void hideVideoOnAChannelReportIsRejected() {
        Report report = Report.builder().id(UUID.randomUUID()).channelId(channel.getId())
                .reporterId(reporterId).reason("SPAM").status(Report.Status.OPEN).build();
        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> service.resolve(report.getId(), ReportService.Action.HIDE_VIDEO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
