package com.wetube.wetube_service.service.interaction;

import com.wetube.wetube_service.dto.CommentDto.CommentRequestDto;
import com.wetube.wetube_service.dto.CommentDto.CommentResponseDto;
import com.wetube.wetube_service.entity.interaction.Comment;

import java.util.List;
import java.util.UUID;

public interface CommentService {

    enum Sort { TOP, NEWEST }

    /** Bình luận gốc của target; bình luận đã ghim luôn đứng đầu. */
    List<CommentResponseDto> getCommentsByTargetId(UUID targetId, Comment.TargetType type, Sort sort);

    /** Chủ của video chứa bình luận (null nếu bình luận không thuộc video). */
    UUID videoOwnerOf(UUID commentId);

    /** Ghim bình luận gốc (bỏ ghim bình luận đang ghim khác của video) hoặc bỏ ghim. */
    CommentResponseDto setPinned(UUID commentId, boolean pinned);

    /** Chủ video thả / bỏ tim. */
    CommentResponseDto setHearted(UUID commentId, boolean hearted, UUID actorId);

    CommentResponseDto getCommentById(UUID id);

    CommentResponseDto createComment(CommentRequestDto request);

    CommentResponseDto updateComment(UUID id, CommentRequestDto request);

    void deleteComment(UUID id);

    List<CommentResponseDto> getReplies(UUID parentId);
}



