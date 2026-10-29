package com.wetube.wetube_service.service.video;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
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
import org.springframework.web.server.ResponseStatusException;

import com.wetube.wetube_service.dto.video.SubtitleCue;
import com.wetube.wetube_service.dto.video.SubtitleManageDto;
import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.entity.video.VideoSubtitle;
import com.wetube.wetube_service.enumeration.SubtitleStatus;
import com.wetube.wetube_service.mapper.video.VideoMapper;
import com.wetube.wetube_service.repository.VideoSubtitleRepository;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.service.CloudinaryService;
import com.wetube.wetube_service.service.video.impl.VideoSubtitleServiceImpl;
import com.wetube.wetube_service.utility.WebVtt;

/** E2: phụ đề AI là bản nháp cho tới khi chủ video công khai; người xem chỉ thấy bản đã công khai. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubtitleReviewTest {

    @Mock VideoSubtitleRepository repo;
    @Mock VideoRepository videoRepo;
    @Mock com.wetube.wetube_service.mapper.video.VideoSubtitleMapper mapper;
    @Mock CloudinaryService cloudinaryService;

    @InjectMocks VideoSubtitleServiceImpl service;

    private final UUID videoId = UUID.randomUUID();
    private Video video;
    private final List<VideoSubtitle> rows = new ArrayList<>();

    @BeforeEach
    void setUp() {
        video = new Video();
        video.setId(videoId);
        when(videoRepo.findById(videoId)).thenReturn(Optional.of(video));
        when(repo.findByVideo_Id(videoId)).thenReturn(rows);
        when(repo.save(any(VideoSubtitle.class))).thenAnswer(inv -> {
            VideoSubtitle s = inv.getArgument(0);
            if (s.getId() == null) s.setId(UUID.randomUUID());
            return s;
        });
        when(cloudinaryService.uploadSubtitle(anyString(), anyString())).thenReturn("https://cdn/v2/sub.vtt");
    }

    private VideoSubtitle row(SubtitleStatus status, String cuesJson, String url) {
        VideoSubtitle s = new VideoSubtitle();
        s.setId(UUID.randomUUID());
        s.setVideo(video);
        s.setLanguage("vi");
        s.setStatus(status);
        s.setCues(cuesJson);
        s.setSubtitleUrl(url);
        rows.add(s);
        when(repo.findByIdAndVideo_Id(s.getId(), videoId)).thenReturn(Optional.of(s));
        when(repo.findById(s.getId())).thenReturn(Optional.of(s));
        return s;
    }

    private static final String ONE_CUE = "[{\"start\":1.0,\"end\":2.5,\"text\":\"Xin chào\",\"original\":\"Hello\"}]";

    @Test
    void viewersOnlySeePublishedSubtitles() {
        row(SubtitleStatus.DRAFT, ONE_CUE, null);
        row(SubtitleStatus.PROCESSING, null, null);
        row(SubtitleStatus.FAILED, null, null);
        // Dòng cũ trước E2: không có status, đã có file
        row(null, null, "https://cdn/old.vtt");
        // Đã ẩn lại: còn file nhưng là bản nháp
        row(SubtitleStatus.DRAFT, null, "https://cdn/hidden.vtt");

        assertThat(rows.stream().filter(VideoSubtitle::isPublished).toList()).hasSize(1);
        service.getSubtitlesByVideo(videoId);
        verify(mapper).toDto(rows.get(3));
        verify(mapper, never()).toDto(rows.get(0));
    }

    @Test
    void videoMapperAlsoHidesDrafts() {
        row(SubtitleStatus.DRAFT, ONE_CUE, null);
        row(SubtitleStatus.PUBLISHED, ONE_CUE, "https://cdn/pub.vtt");
        VideoMapper videoMapper = org.mapstruct.factory.Mappers.getMapper(VideoMapper.class);

        assertThat(videoMapper.mapSubtitles(rows)).extracting("subtitleUrl").containsExactly("https://cdn/pub.vtt");
    }

    @Test
    void startAiReplacesSameLanguageAndStartsProcessing() {
        VideoSubtitle old = row(SubtitleStatus.PUBLISHED, ONE_CUE, "https://cdn/pub.vtt");
        when(repo.findByVideo_IdAndLanguage(videoId, "vi")).thenReturn(List.of(old));

        SubtitleManageDto dto = service.startAi(videoId, "vi");

        verify(repo).deleteAll(List.of(old));
        assertThat(dto.getStatus()).isEqualTo(SubtitleStatus.PROCESSING);
        assertThat(dto.isAiGenerated()).isTrue();
    }

    @Test
    void startAiRefusesWhileSameLanguageIsProcessing() {
        VideoSubtitle running = row(SubtitleStatus.PROCESSING, null, null);
        when(repo.findByVideo_IdAndLanguage(videoId, "vi")).thenReturn(List.of(running));

        assertThatThrownBy(() -> service.startAi(videoId, "vi")).isInstanceOf(ResponseStatusException.class);
        verify(repo, never()).deleteAll(any());
    }

    @Test
    void completeAiMakesDraftWithoutUploading() {
        VideoSubtitle s = row(SubtitleStatus.PROCESSING, null, null);

        service.completeAi(s.getId(), List.of(new SubtitleCue(1, 2, "Xin chào", "Hello")));

        assertThat(s.getStatus()).isEqualTo(SubtitleStatus.DRAFT);
        assertThat(s.getCues()).contains("Xin chào").contains("Hello");
        assertThat(s.isPublished()).isFalse();
        verify(cloudinaryService, never()).uploadSubtitle(anyString(), anyString());
    }

    @Test
    void publishUploadsVttBuiltFromCues() {
        VideoSubtitle s = row(SubtitleStatus.DRAFT, ONE_CUE, null);

        service.publish(videoId, s.getId());

        ArgumentCaptor<String> vtt = ArgumentCaptor.forClass(String.class);
        verify(cloudinaryService).uploadSubtitle(vtt.capture(), eq("subtitles/" + videoId + "_vi.vtt"));
        assertThat(vtt.getValue()).isEqualTo("WEBVTT\n\n00:00:01.000 --> 00:00:02.500\nXin chào\n\n");
        assertThat(s.getStatus()).isEqualTo(SubtitleStatus.PUBLISHED);
        assertThat(s.getSubtitleUrl()).isEqualTo("https://cdn/v2/sub.vtt");
    }

    @Test
    void cannotPublishWhileProcessing() {
        VideoSubtitle s = row(SubtitleStatus.PROCESSING, null, null);

        assertThatThrownBy(() -> service.publish(videoId, s.getId())).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void savingDraftDoesNotUploadButSavingPublishedDoes() {
        VideoSubtitle draft = row(SubtitleStatus.DRAFT, ONE_CUE, null);
        List<SubtitleCue> edited = List.of(new SubtitleCue(3, 4, "  Sau  ", null), new SubtitleCue(0.5, 1, "Trước", null));

        SubtitleManageDto dto = service.saveCues(videoId, draft.getId(), edited);

        verify(cloudinaryService, never()).uploadSubtitle(anyString(), anyString());
        // Xếp theo giờ, bỏ khoảng trắng thừa
        assertThat(dto.getCues()).extracting(SubtitleCue::getText).containsExactly("Trước", "Sau");

        VideoSubtitle published = row(SubtitleStatus.PUBLISHED, ONE_CUE, "https://cdn/v1/sub.vtt");
        service.saveCues(videoId, published.getId(), edited);
        verify(cloudinaryService).uploadSubtitle(anyString(), anyString());
        assertThat(published.getSubtitleUrl()).isEqualTo("https://cdn/v2/sub.vtt");
    }

    @Test
    void unpublishHidesButKeepsFileForOldSubtitles() {
        VideoSubtitle old = row(null, null, "https://cdn/old.vtt");

        service.unpublish(videoId, old.getId());
        assertThat(old.isPublished()).isFalse();

        service.publish(videoId, old.getId());
        assertThat(old.isPublished()).isTrue();
        verify(cloudinaryService, never()).uploadSubtitle(anyString(), anyString());
    }

    @Test
    void validateRejectsBadTimesAndDropsEmptyCues() {
        assertThatThrownBy(() -> service.saveCues(videoId, row(SubtitleStatus.DRAFT, ONE_CUE, null).getId(),
                List.of(new SubtitleCue(5, 5, "x", null)))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.saveCues(videoId, row(SubtitleStatus.DRAFT, ONE_CUE, null).getId(),
                List.of(new SubtitleCue(-1, 2, "x", null)))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.saveCues(videoId, row(SubtitleStatus.DRAFT, ONE_CUE, null).getId(),
                List.of(new SubtitleCue(1, 2, "x".repeat(501), null)))).isInstanceOf(IllegalArgumentException.class);

        SubtitleManageDto dto = service.saveCues(videoId, row(SubtitleStatus.DRAFT, ONE_CUE, null).getId(),
                List.of(new SubtitleCue(1, 2, "   ", null), new SubtitleCue(2, 3, "Còn", null)));
        assertThat(dto.getCueCount()).isEqualTo(1);
    }

    @Test
    void webVttKeepsCueBlocksIntact() {
        String vtt = WebVtt.build(List.of(new SubtitleCue(3661.25, 3662, "a --> b\n\nc", null)));

        assertThat(vtt).isEqualTo("WEBVTT\n\n01:01:01.250 --> 01:01:02.000\na -> b\nc\n\n");
    }
}
