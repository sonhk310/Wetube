package com.wetube.wetube_service.service.interaction;

import com.wetube.wetube_service.dto.CommentDto.CommentRequestDto;
import com.wetube.wetube_service.dto.CommentDto.CommentResponseDto;
import com.wetube.wetube_service.entity.interaction.Comment;

import java.util.List;
import java.util.UUID;

public interface CommentService {

    List<CommentResponseDto> getCommentsByTargetId(UUID targetId, Comment.TargetType type);

    CommentResponseDto getCommentById(UUID id);

    CommentResponseDto createComment(CommentRequestDto request);

    CommentResponseDto updateComment(UUID id, CommentRequestDto request);

    void deleteComment(UUID id);

    List<CommentResponseDto> getReplies(UUID parentId);
}



