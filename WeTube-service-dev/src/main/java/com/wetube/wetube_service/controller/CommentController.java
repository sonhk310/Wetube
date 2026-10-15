package com.wetube.wetube_service.controller;

import com.wetube.wetube_service.dto.CommentDto.CommentRequestDto;
import com.wetube.wetube_service.dto.CommentDto.CommentResponseDto;
import com.wetube.wetube_service.entity.interaction.Comment;
import com.wetube.wetube_service.service.interaction.CommentService;
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

    @GetMapping("/{targetType}/{targetId}")
    public ResponseEntity<List<CommentResponseDto>> getCommentsByTarget(
            @PathVariable("targetId") UUID targetId,
            @PathVariable("targetType") Comment.TargetType targetType
    ) {
        return ResponseEntity.ok(commentService.getCommentsByTargetId(targetId, targetType));
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
    public ResponseEntity<CommentResponseDto> createComment(@RequestBody CommentRequestDto request) {
        return ResponseEntity.ok(commentService.createComment(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommentResponseDto> updateComment(@PathVariable UUID id, @RequestBody CommentRequestDto request) {
        return ResponseEntity.ok(commentService.updateComment(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable UUID id) {
        commentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }
}

