package com.wetube.wetube_service.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.wetube.wetube_service.security.CurrentUser;
import com.wetube.wetube_service.security.OwnershipGuard;
import com.wetube.wetube_service.security.RateLimiter;
import com.wetube.wetube_service.dto.CommentDto.CommentRequestDto;
import com.wetube.wetube_service.dto.CommentDto.CommentResponseDto;
import com.wetube.wetube_service.entity.interaction.Comment;
import com.wetube.wetube_service.service.interaction.CommentService;
import com.wetube.wetube_service.service.video.TagAffinityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final OwnershipGuard ownershipGuard;
    private final RateLimiter rateLimiter;
    private final TagAffinityService tagAffinityService;

    /** sort=top (mặc định: nhiều thích + trả lời trước) hoặc newest. Bình luận đã ghim luôn đứng đầu. */
    @GetMapping("/{targetType}/{targetId}")
    public ResponseEntity<List<CommentResponseDto>> getCommentsByTarget(
            @PathVariable("targetId") UUID targetId,
            @PathVariable("targetType") Comment.TargetType targetType,
            @RequestParam(value = "sort", defaultValue = "top") String sort
    ) {
        CommentService.Sort order = "newest".equalsIgnoreCase(sort) ? CommentService.Sort.NEWEST : CommentService.Sort.TOP;
        return ResponseEntity.ok(commentService.getCommentsByTargetId(targetId, targetType, order));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommentResponseDto> getCommentById(@PathVariable UUID id) {
        return ResponseEntity.ok(commentService.getCommentById(id));
    }

    @GetMapping("/reply/{parentId}")
    public ResponseEntity<List<CommentResponseDto>> getReplies(@PathVariable UUID parentId) {
        return ResponseEntity.ok(commentService.getReplies(parentId));
    }

    @PostMapping
    public ResponseEntity<CommentResponseDto> createComment(@RequestBody CommentRequestDto request, @AuthenticationPrincipal Jwt jwt) {
        request.setUserId(CurrentUser.id(jwt));
        rateLimiter.check(RateLimiter.Limit.COMMENT, request.getUserId());
        CommentResponseDto created = commentService.createComment(request);
        if (request.getParentCommentId() == null && request.getTargetType() == Comment.TargetType.VIDEO
                && commentService.countRootCommentsBy(request.getUserId(), Comment.TargetType.VIDEO, request.getTargetId()) == 1) {
            tagAffinityService.addQuietly(request.getUserId(), request.getTargetId(), TagAffinityService.COMMENT);
        }
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommentResponseDto> updateComment(@PathVariable UUID id, @RequestBody CommentRequestDto request, @AuthenticationPrincipal Jwt jwt) {
        ownershipGuard.commentOwner(id, jwt);
        request.setUserId(CurrentUser.id(jwt));
        return ResponseEntity.ok(commentService.updateComment(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        ownershipGuard.commentRemovable(id, jwt);
        commentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }

    /** Chủ video ghim bình luận gốc lên đầu (thay bình luận đang ghim, nếu có). */
    @PostMapping("/{id}/pin")
    public ResponseEntity<CommentResponseDto> pin(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        ownershipGuard.commentVideoOwner(id, jwt);
        return ResponseEntity.ok(commentService.setPinned(id, true));
    }

    @DeleteMapping("/{id}/pin")
    public ResponseEntity<CommentResponseDto> unpin(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        ownershipGuard.commentVideoOwner(id, jwt);
        return ResponseEntity.ok(commentService.setPinned(id, false));
    }

    /** Chủ video thả tim bình luận. */
    @PostMapping("/{id}/heart")
    public ResponseEntity<CommentResponseDto> heart(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        ownershipGuard.commentVideoOwner(id, jwt);
        return ResponseEntity.ok(commentService.setHearted(id, true, CurrentUser.id(jwt)));
    }

    @DeleteMapping("/{id}/heart")
    public ResponseEntity<CommentResponseDto> unheart(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        ownershipGuard.commentVideoOwner(id, jwt);
        return ResponseEntity.ok(commentService.setHearted(id, false, CurrentUser.id(jwt)));
    }
}

