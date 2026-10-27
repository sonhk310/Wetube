package com.wetube.wetube_service.controller;

import com.wetube.wetube_service.dto.request.ChannelUpdateRequest;
import com.wetube.wetube_service.dto.response.ChannelResponseDto;
import com.wetube.wetube_service.dto.response.ChannelSearchDto;
import com.wetube.wetube_service.dto.response.HandleCheckDto;
import com.wetube.wetube_service.security.CurrentUser;
import com.wetube.wetube_service.security.RateLimiter;
import com.wetube.wetube_service.service.channel.ChannelSearchService;
import com.wetube.wetube_service.service.channel.ChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
public class ChannelController {
    private final ChannelService channelService;
    private final ChannelSearchService channelSearchService;
    private final RateLimiter rateLimiter;

    /** Tìm kênh theo tên (bộ lọc "Kênh" ở trang tìm kiếm). */
    @GetMapping("/search")
    public ResponseEntity<List<ChannelSearchDto>> search(@RequestParam("q") String query,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "false") boolean strict) {
        return ResponseEntity.ok(channelSearchService.search(query, limit, strict));
    }

    /** Kênh theo id (UUID) hoặc theo @handle (không có "@"); trang /@handle của client gọi bằng handle. */
    @GetMapping("/{channel}")
    public ResponseEntity<ChannelResponseDto> getChannel(@PathVariable String channel, @AuthenticationPrincipal Jwt jwt) {
        UUID viewerId = (jwt != null) ? UUID.fromString(jwt.getSubject()) : null;
        UUID channelId = parseUuid(channel);
        ChannelResponseDto response = channelId != null
                ? channelService.getChannelById(channelId, viewerId, CurrentUser.isAdmin(jwt))
                : channelService.getChannelByHandle(channel, viewerId, CurrentUser.isAdmin(jwt));
        return ResponseEntity.ok(response);
    }

    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Trang kênh theo @handle (không có "@" trong đường dẫn). */
    @GetMapping("/handle/{handle}")
    public ResponseEntity<ChannelResponseDto> getChannelByHandle(@PathVariable String handle, @AuthenticationPrincipal Jwt jwt) {
        UUID viewerId = (jwt != null) ? UUID.fromString(jwt.getSubject()) : null;
        return ResponseEntity.ok(channelService.getChannelByHandle(handle, viewerId, CurrentUser.isAdmin(jwt)));
    }

    @GetMapping("/handle-check")
    public ResponseEntity<HandleCheckDto> checkHandle(@RequestParam("handle") String handle, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(channelService.checkHandle(CurrentUser.id(jwt), handle));
    }

    @PutMapping("/me/handle")
    public ResponseEntity<ChannelResponseDto> changeHandle(@RequestBody HandleRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(channelService.changeHandle(CurrentUser.id(jwt), request.handle()));
    }

    /** Sửa tên / mô tả / ảnh của kênh mình (xem ChannelUpdateRequest). */
    @PutMapping("/me")
    public ResponseEntity<ChannelResponseDto> updateMyChannel(@RequestBody ChannelUpdateRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(channelService.updateChannel(CurrentUser.id(jwt), request));
    }

    /** Tải ảnh bìa / ảnh đại diện lên; trả {"url"} để gửi tiếp qua PUT /me. */
    @PostMapping(value = "/me/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadMyImage(@RequestParam("file") MultipartFile file,
            @RequestParam("kind") String kind, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = CurrentUser.id(jwt);
        rateLimiter.check(RateLimiter.Limit.CHANNEL_IMAGE, userId);
        return ResponseEntity.ok(Map.of("url", channelService.uploadChannelImage(userId, file, kind)));
    }

    public record HandleRequest(String handle) {
    }
}
