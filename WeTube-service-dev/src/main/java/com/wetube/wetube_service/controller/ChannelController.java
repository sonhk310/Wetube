package com.wetube.wetube_service.controller;

import com.wetube.wetube_service.dto.response.ChannelResponseDto;
import com.wetube.wetube_service.dto.response.ChannelSearchDto;
import com.wetube.wetube_service.dto.response.HandleCheckDto;
import com.wetube.wetube_service.security.CurrentUser;
import com.wetube.wetube_service.service.channel.ChannelSearchService;
import com.wetube.wetube_service.service.channel.ChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
public class ChannelController {
    private final ChannelService channelService;
    private final ChannelSearchService channelSearchService;

    /** Tìm kênh theo tên (bộ lọc "Kênh" ở trang tìm kiếm). */
    @GetMapping("/search")
    public ResponseEntity<List<ChannelSearchDto>> search(@RequestParam("q") String query,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "false") boolean strict) {
        return ResponseEntity.ok(channelSearchService.search(query, limit, strict));
    }

    @GetMapping("/{channelId}")
    public ResponseEntity<ChannelResponseDto> getChannelById(@PathVariable UUID channelId, @AuthenticationPrincipal Jwt jwt) {
        UUID viewerId = (jwt != null) ? UUID.fromString(jwt.getSubject()) : null;
        ChannelResponseDto response = channelService.getChannelById(channelId, viewerId);
        return ResponseEntity.ok(response);
    }

    /** Trang kênh theo @handle (không có "@" trong đường dẫn). */
    @GetMapping("/handle/{handle}")
    public ResponseEntity<ChannelResponseDto> getChannelByHandle(@PathVariable String handle, @AuthenticationPrincipal Jwt jwt) {
        UUID viewerId = (jwt != null) ? UUID.fromString(jwt.getSubject()) : null;
        return ResponseEntity.ok(channelService.getChannelByHandle(handle, viewerId));
    }

    @GetMapping("/handle-check")
    public ResponseEntity<HandleCheckDto> checkHandle(@RequestParam("handle") String handle, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(channelService.checkHandle(CurrentUser.id(jwt), handle));
    }

    @PutMapping("/me/handle")
    public ResponseEntity<ChannelResponseDto> changeHandle(@RequestBody HandleRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(channelService.changeHandle(CurrentUser.id(jwt), request.handle()));
    }

    public record HandleRequest(String handle) {
    }
}
