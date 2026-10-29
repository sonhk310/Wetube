package com.wetube.wetube_service.controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wetube.wetube_service.dto.request.CreateSubtitleRequest;
import com.wetube.wetube_service.dto.video.SubtitleCue;
import com.wetube.wetube_service.dto.video.SubtitleManageDto;
import com.wetube.wetube_service.dto.video.VideoSubtitleDto;
import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.security.CurrentUser;
import com.wetube.wetube_service.security.OwnershipGuard;
import com.wetube.wetube_service.security.RateLimiter;
import com.wetube.wetube_service.service.AI.SubtitleAiService;
import com.wetube.wetube_service.service.video.VideoSubtitleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoSubtitleController {

    private final VideoSubtitleService subtitleService;
    private final SubtitleAiService subtitleAiService;
    private final VideoRepository videoRepository;
    private final OwnershipGuard ownershipGuard;
    private final RateLimiter rateLimiter;

    /** Lượt phụ đề AI của cả trang tính theo ngày giờ Việt Nam. */
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    @PostMapping("/{videoId}/subtitles")
    public ResponseEntity<VideoSubtitleDto> addSubtitle(
            @PathVariable UUID videoId,
            @RequestBody CreateSubtitleRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        ownershipGuard.videoOwner(videoId, jwt);
        VideoSubtitleDto dto = subtitleService.addSubtitle(videoId, request);
        return ResponseEntity.ok(dto);
    }

    /**
     * Chủ video yêu cầu tạo phụ đề AI (Gemini); trả 202 kèm dòng phụ đề PROCESSING. AI chạy nền,
     * xong thì phụ đề là bản nháp (client hỏi lại GET /subtitles/manage).
     */
    @PostMapping("/{videoId}/subtitles/ai")
    public ResponseEntity<SubtitleManageDto> generateAiSubtitle(
            @PathVariable UUID videoId,
            @RequestParam String targetLang,
            @AuthenticationPrincipal Jwt jwt) {
        ownershipGuard.videoOwner(videoId, jwt);
        if (!SubtitleAiService.isSupported(targetLang)) {
            throw new IllegalArgumentException("Ngôn ngữ chưa hỗ trợ: " + targetLang);
        }
        Video video = videoRepository.findById(videoId).orElseThrow();
        if (SubtitleAiService.isTooLong(video.getDuration())) {
            throw new IllegalArgumentException("Chỉ tạo được phụ đề AI cho video dài tối đa "
                    + SubtitleAiService.MAX_DURATION_SECONDS / 60 + " phút.");
        }
        rateLimiter.check(RateLimiter.Limit.AI_SUBTITLE, CurrentUser.id(jwt));
        rateLimiter.check(RateLimiter.Limit.AI_SUBTITLE_DAILY, LocalDate.now(VN));
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(subtitleAiService.start(videoId, video.getVideoUrl(), targetLang));
    }

    // Quản lý phụ đề (E2): chỉ chủ video

    @GetMapping("/{videoId}/subtitles/manage")
    public List<SubtitleManageDto> listSubtitles(@PathVariable UUID videoId, @AuthenticationPrincipal Jwt jwt) {
        ownershipGuard.videoOwner(videoId, jwt);
        return subtitleService.list(videoId);
    }

    @GetMapping("/{videoId}/subtitles/{subtitleId}")
    public SubtitleManageDto getSubtitle(@PathVariable UUID videoId, @PathVariable UUID subtitleId,
            @AuthenticationPrincipal Jwt jwt) {
        ownershipGuard.videoOwner(videoId, jwt);
        return subtitleService.get(videoId, subtitleId);
    }

    @PutMapping("/{videoId}/subtitles/{subtitleId}")
    public SubtitleManageDto saveSubtitle(@PathVariable UUID videoId, @PathVariable UUID subtitleId,
            @RequestBody List<SubtitleCue> cues, @AuthenticationPrincipal Jwt jwt) {
        ownershipGuard.videoOwner(videoId, jwt);
        return subtitleService.saveCues(videoId, subtitleId, cues);
    }

    @PostMapping("/{videoId}/subtitles/{subtitleId}/publish")
    public SubtitleManageDto publishSubtitle(@PathVariable UUID videoId, @PathVariable UUID subtitleId,
            @AuthenticationPrincipal Jwt jwt) {
        ownershipGuard.videoOwner(videoId, jwt);
        return subtitleService.publish(videoId, subtitleId);
    }

    @PostMapping("/{videoId}/subtitles/{subtitleId}/unpublish")
    public SubtitleManageDto unpublishSubtitle(@PathVariable UUID videoId, @PathVariable UUID subtitleId,
            @AuthenticationPrincipal Jwt jwt) {
        ownershipGuard.videoOwner(videoId, jwt);
        return subtitleService.unpublish(videoId, subtitleId);
    }

    @DeleteMapping("/{videoId}/subtitles/{subtitleId}")
    public ResponseEntity<Void> deleteSubtitle(@PathVariable UUID videoId, @PathVariable UUID subtitleId,
            @AuthenticationPrincipal Jwt jwt) {
        ownershipGuard.videoOwner(videoId, jwt);
        subtitleService.delete(videoId, subtitleId);
        return ResponseEntity.noContent().build();
    }
}
