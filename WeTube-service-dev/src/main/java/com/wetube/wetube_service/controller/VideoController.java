package com.wetube.wetube_service.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.wetube.wetube_service.dto.response.CustomPageResponse;
import com.wetube.wetube_service.dto.response.SearchResponseDto;
import com.wetube.wetube_service.dto.video.VideoFormDetailDto;
import com.wetube.wetube_service.dto.video.VideoUpdateDto;
import com.wetube.wetube_service.dto.video.VisibilityUpdateRequest;
import com.wetube.wetube_service.dto.LikeDto;
import com.wetube.wetube_service.security.CurrentUser;
import com.wetube.wetube_service.security.OwnershipGuard;
import com.wetube.wetube_service.security.RateLimiter;
import com.wetube.wetube_service.security.VideoAccess;
import com.wetube.wetube_service.service.interaction.LikeService;
import jakarta.servlet.http.HttpServletRequest;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.wetube.wetube_service.dto.video.VideoDetailResponseDto;
import com.wetube.wetube_service.dto.video.VideoDto;
import com.wetube.wetube_service.search.SearchFilters;
import com.wetube.wetube_service.search.VideoDocument;
import com.wetube.wetube_service.service.VideoSearchService;
import com.wetube.wetube_service.service.AI.SubtitleAiService;
import com.wetube.wetube_service.service.video.VideoDownloadService;
import com.wetube.wetube_service.service.video.VideoService;
import com.wetube.wetube_service.service.video.WatchProgressService;
import com.wetube.wetube_service.service.analytics.WatchTimeTracker;

import com.wetube.wetube_service.service.video.TagAffinityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private final VideoService videoService;
    private final VideoSearchService searchService;
    private final SubtitleAiService subtitleAiService;
    private final OwnershipGuard ownershipGuard;
    private final LikeService likeService;
    private final VideoAccess videoAccess;
    private final RateLimiter rateLimiter;
    private final VideoDownloadService videoDownloadService;
    private final WatchProgressService watchProgressService;
    private final WatchTimeTracker watchTimeTracker;
    private final TagAffinityService tagAffinityService;

    private static final PolicyFactory SANITIZER_POLICY = Sanitizers.FORMATTING.and(Sanitizers.LINKS);

    @PostMapping(value = "/uploadFile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Object> uploadFile(
            @RequestParam("videoFile") MultipartFile videoFile,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(value = "thumbnailFile", required = false) MultipartFile thumbnailFile,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "duration", required = false) Float duration,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "targetLang", required = false) String targetLang,
            @RequestParam(value = "visibility", required = false) String visibility) {
        try {
        // 🟢 Debug log toàn bộ dữ liệu FE gửi sang
        System.out.println("========== FE gửi lên ==========");
        System.out.println("📂 videoFile: " + (videoFile != null ? videoFile.getOriginalFilename() : "null"));
        System.out.println("🖼 thumbnailFile: " + (thumbnailFile != null ? thumbnailFile.getOriginalFilename() : "null"));
        System.out.println("📝 title: " + title);
        System.out.println("📝 description: " + description);
        System.out.println("🏷 tags: " + tags);
        System.out.println("⏱ duration: " + duration);
        System.out.println("🌍 targetLang: " + targetLang);
        System.out.println("================================");

            UUID authenticatedUserId = UUID.fromString(jwt.getSubject());

            // Sanitize input
            String sanitizedTitle = SANITIZER_POLICY.sanitize(title);
            String sanitizedDescription = (description != null) ? SANITIZER_POLICY.sanitize(description) : null;
            String sanitizedTags = (tags != null) ? SANITIZER_POLICY.sanitize(tags) : null;

            // Meta info
            VideoDto meta = VideoDto.builder()
                    .title(sanitizedTitle)
                    .description(sanitizedDescription)
                    .duration(duration)
                    .tagsAsString(sanitizedTags)
                    .videosStatus(visibility)
                    .build();

            // Tạo video chính
            VideoDto createdVideo = videoService.createVideo(videoFile, thumbnailFile, meta, authenticatedUserId);

            // Có chọn ngôn ngữ thì tạo phụ đề AI (Gemini) chạy nền, không chặn upload; quá giới hạn thì bỏ qua phụ đề
            if (SubtitleAiService.isSupported(targetLang)) {
                if (rateLimiter.tryAcquire(RateLimiter.Limit.AI_SUBTITLE, authenticatedUserId)) {
                    subtitleAiService.start(createdVideo.getId(), createdVideo.getVideoUrl(), targetLang);
                } else {
                    log.warn("Bỏ qua phụ đề AI cho video {}: người dùng {} đã vượt giới hạn", createdVideo.getId(), authenticatedUserId);
                }
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(createdVideo);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Bad Request", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Server Error", "message", e.getMessage()));
        }
    }

    /**
     * Bước 1 của luồng tải lên: chọn tệp là tải lên ngay thành bản nháp (chỉ chủ video thấy).
     * Chi tiết và chế độ hiển thị lưu sau bằng PUT /api/videos/{id}.
     */
    @PostMapping(value = "/drafts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VideoDto> createDraft(
            @RequestParam("videoFile") MultipartFile videoFile,
            @RequestParam(value = "duration", required = false) Float duration,
            @RequestParam(value = "width", required = false) Integer width,
            @RequestParam(value = "height", required = false) Integer height,
            @AuthenticationPrincipal Jwt jwt) {
        VideoDto draft = videoService.createDraft(videoFile, duration, width, height, CurrentUser.id(jwt));
        return ResponseEntity.status(HttpStatus.CREATED).body(draft);
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<VideoDetailResponseDto> getDetail(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = (jwt != null) ? UUID.fromString(jwt.getSubject()) : null;
        return ResponseEntity.ok(videoService.getDetailWithRecommend(id, userId, CurrentUser.isAdmin(jwt)));
    }

    @PostMapping(value = "/{id}/tags", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VideoDto> addTagsToVideo(
            @PathVariable("id") UUID videoId,
            @RequestParam("hashtags") String hashtags,
            @AuthenticationPrincipal Jwt jwt) {
        ownershipGuard.videoOwner(videoId, jwt);
        if (hashtags == null || hashtags.isBlank()) {
            return ResponseEntity.badRequest().body(null);
        }
        return ResponseEntity.ok(videoService.addTags(videoId, hashtags));
    }

    @GetMapping
    public ResponseEntity<Object> getAllVideo() {
        return ResponseEntity.ok(videoService.getAllVideo());
    }

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<CustomPageResponse<VideoDto>> getVideosByUserId(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "all") String type,
            @RequestParam(defaultValue = "latest") String sort,
            @AuthenticationPrincipal Jwt jwt) {

        UUID viewerId = (jwt != null) ? UUID.fromString(jwt.getSubject()) : null;
        CustomPageResponse<VideoDto> videoPage = videoService.getVideosByUserId(
                userId, page, Math.min(size, 50), viewerId, CurrentUser.isAdmin(jwt), type, sort);
        return ResponseEntity.ok(videoPage);
    }

    @GetMapping("/shorts")
    public ResponseEntity<CustomPageResponse<VideoDto>> getShorts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(videoService.getShorts(page, Math.min(size, 50)));
    }

    @GetMapping("/by-tag")
    public ResponseEntity<CustomPageResponse<VideoDto>> getVideosByTag(
            @RequestParam String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(videoService.getVideosByTag(tag, page, Math.min(size, 50)));
    }

    // ===================== Elasticsearch Search =====================

    // Search gần đúng theo title → chỉ trả về VideoDocument
    @GetMapping("/search/videos")
    public Page<VideoDocument> searchByTitle(@RequestParam String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return searchService.searchByTitle(title, page, size);
    }

    // Search chính xác title
    @GetMapping("/search/videos/exact")
    public VideoDocument searchExact(@RequestParam String title) {
        return searchService.searchExactTitle(title);
    }

    // Search đầy đủ (ES + load lại từ MySQL) → trả về VideoDto đầy đủ
    @GetMapping("/search/full")
    public Page<VideoDto> searchByTitleFull(@RequestParam String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String duration,
            @RequestParam(required = false) String uploaded,
            @RequestParam(required = false) String sort) {
        return searchService.searchByTitleFull(title, SearchFilters.of(type, duration, uploaded, sort),
                page, Math.min(size, 50));
    }

    // "Video tương tự" dưới kết quả tìm kiếm; bỏ các video đã nằm trong trang kết quả chính (size)
    @GetMapping("/search/similar")
    public List<VideoDto> searchSimilar(@RequestParam String title,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type) {
        return searchService.similarVideos(title, SearchFilters.of(type, null, null, null), Math.min(size, 50), 8);
    }

    // Suggest autocomplete
    @GetMapping("/search/suggest")
    public ResponseEntity<SearchResponseDto<List<String>>> suggest(
            @RequestParam String prefix,
            @RequestParam(defaultValue = "5") int size) {
        return ResponseEntity.ok(SearchResponseDto.ok(searchService.suggestTitles(prefix, size)));
    }
    // ===================== Detail + Update =====================

    @GetMapping("/{id}/form-details")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VideoFormDetailDto> getVideoDetail(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        ownershipGuard.videoOwner(id, jwt);
        return ResponseEntity.ok(videoService.getVideoDetail(id));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VideoDto> updateVideoDetails(
            @PathVariable UUID id,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("status") String status,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "searchKeywords", required = false) String searchKeywords,
            @RequestParam(value = "topicId", required = false) String topicId,
            @RequestParam(value = "thumbnailFile", required = false) MultipartFile thumbnailFile,
            @AuthenticationPrincipal Jwt jwt) {

        ownershipGuard.videoOwner(id, jwt);
        UUID authenticatedUserId = UUID.fromString(jwt.getSubject());

        VideoUpdateDto updateDto = new VideoUpdateDto();
        updateDto.setTitle(title);
        updateDto.setDescription(description);
        updateDto.setStatus(status);
        updateDto.setTags(tags);
        updateDto.setSearchKeywords(searchKeywords);
        updateDto.setTopicId(topicId);

        VideoDto updatedVideo = videoService.updateVideo(id, updateDto, thumbnailFile, authenticatedUserId);

        return ResponseEntity.ok(updatedVideo);
    }

    /** Xoá mềm video (chủ video hoặc admin). */
    /** Studio, chọn nhiều hàng: đổi chế độ hiển thị (chỉ video của mình; bỏ qua bản nháp / bị ẩn). */
    @PatchMapping("/visibility")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Integer>> updateVisibility(@RequestBody VisibilityUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        int updated = videoService.updateVisibility(request.videoIds(), request.status(), CurrentUser.id(jwt));
        return ResponseEntity.ok(Map.of("updated", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> deleteVideo(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        ownershipGuard.videoOwner(id, jwt);
        videoService.deleteVideo(id);
        return ResponseEntity.ok(Map.of("message", "Đã xoá video"));
    }

    /**
     * Tải video về máy (Premium): chuyển hướng 302 tới link Cloudinary có flag tải về, backend không chuyển dữ liệu.
     * 401 chưa đăng nhập, 403 chưa có Premium, 404 không xem được video, 429 quá 30 lần mỗi giờ.
     */
    @GetMapping("/{id}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> download(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        String url = videoDownloadService.downloadUrl(id, jwt);
        return ResponseEntity.status(HttpStatus.FOUND).location(java.net.URI.create(url)).build();
    }

    /**
     * Ghi nhận một lượt xem, không cần đăng nhập. Mỗi người xem (tài khoản, hoặc anonId do trình duyệt tạo,
     * hoặc IP nếu thiếu) chỉ được tính 1 lần cho mỗi video trong 30 phút.
     */
    @PostMapping("/{id}/view")
    public ResponseEntity<Map<String, Object>> registerView(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        rateLimiter.check(RateLimiter.Limit.VIEW, clientIp(request));
        UUID userId = (jwt != null) ? UUID.fromString(jwt.getSubject()) : null;
        String viewerKey;
        if (userId != null) {
            viewerKey = "u:" + userId;
        } else {
            String anonId = body == null ? null : body.get("anonId");
            viewerKey = (anonId != null && !anonId.isBlank() && anonId.length() <= 64)
                    ? "a:" + anonId.trim()
                    : "ip:" + clientIp(request);
        }
        Map<String, Object> result = videoService.registerView(id, viewerKey, userId, CurrentUser.isAdmin(jwt));
        if (userId != null && Boolean.TRUE.equals(result.get("counted"))) {
            tagAffinityService.addQuietly(userId, id, TagAffinityService.VIEW);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Trình phát báo vị trí đang xem (người đã đăng nhập): lưu vào lịch sử để phát tiếp và vẽ thanh đỏ.
     * Tham số nằm trên URL, không có body, để trình duyệt gửi được cả khi đóng tab (fetch keepalive, không preflight).
     * final=true khi dừng / hết video / rời trang: ghi MySQL ngay.
     */
    @PostMapping("/{id}/progress")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> saveProgress(
            @PathVariable UUID id,
            @RequestParam("t") float seconds,
            @RequestParam(value = "final", defaultValue = "false") boolean flush,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = CurrentUser.id(jwt);
        rateLimiter.check(RateLimiter.Limit.PROGRESS, userId);
        if (!Float.isFinite(seconds) || seconds < 0) {
            throw new IllegalArgumentException("Thời điểm không hợp lệ");
        }
        UUID ownerId = videoAccess.requireViewable(id, jwt);
        // Thời gian xem cho chủ kênh: tính cả khi người xem tạm dừng lưu lịch sử
        watchTimeTracker.record("u:" + userId, id, ownerId, seconds);
        watchProgressService.record(userId, id, seconds, flush);
        return ResponseEntity.noContent().build();
    }

    /**
     * Trình phát của khách chưa đăng nhập báo vị trí đang xem: chỉ để tính thời gian xem cho chủ kênh
     * (không lưu lịch sử). Tham số trên URL để gửi được bằng fetch keepalive khi đóng tab.
     */
    @PostMapping("/{id}/watch-time")
    public ResponseEntity<Void> watchTime(
            @PathVariable UUID id,
            @RequestParam("t") float seconds,
            @RequestParam(value = "anonId", required = false) String anonId,
            HttpServletRequest request) {
        rateLimiter.check(RateLimiter.Limit.PROGRESS, clientIp(request));
        if (!Float.isFinite(seconds) || seconds < 0) {
            throw new IllegalArgumentException("Thời điểm không hợp lệ");
        }
        UUID ownerId = videoAccess.requireViewable(id, null);
        String viewerKey = (anonId != null && !anonId.isBlank() && anonId.length() <= 64)
                ? "a:" + anonId.trim()
                : "ip:" + clientIp(request);
        watchTimeTracker.record(viewerKey, id, ownerId, seconds);
        return ResponseEntity.noContent().build();
    }

    /** Vị trí đã xem của nhiều video (thanh đỏ trên thumbnail). Chưa đăng nhập thì trả rỗng. */
    @GetMapping("/progress")
    public ResponseEntity<Map<UUID, Float>> getProgress(
            @RequestParam("ids") List<UUID> ids,
            @AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.ok(Map.of());
        }
        if (ids.size() > 100) {
            throw new IllegalArgumentException("Tối đa 100 video mỗi lần");
        }
        return ResponseEntity.ok(watchProgressService.getMany(CurrentUser.id(jwt), ids));
    }

    /** Bật/tắt "không thích" của chính người dùng; chỉ trả về trạng thái của họ, không có số lượng. */
    @PostMapping("/{id}/dislike")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LikeDto> toggleDislike(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        videoAccess.requireViewable(id, jwt);
        return ResponseEntity.ok(likeService.toggleDislike(id, CurrentUser.id(jwt)));
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
