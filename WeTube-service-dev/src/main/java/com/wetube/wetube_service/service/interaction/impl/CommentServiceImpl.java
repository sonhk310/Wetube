package com.wetube.wetube_service.service.interaction.impl;

import com.wetube.wetube_service.dto.CommentDto.CommentRequestDto;
import com.wetube.wetube_service.dto.CommentDto.CommentResponseDto;
import com.wetube.wetube_service.dto.UserDto;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.interaction.Comment;
import com.wetube.wetube_service.entity.interaction.Like;
import com.wetube.wetube_service.mapper.UserMapper;
import com.wetube.wetube_service.mapper.interaction.CommentMapper;
import com.wetube.wetube_service.repository.UserRepository;
import com.wetube.wetube_service.repository.interaction.CommentRepository;
import com.wetube.wetube_service.repository.post.PostRepository;
import com.wetube.wetube_service.service.interaction.CommentService;
import com.wetube.wetube_service.service.interaction.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final PostRepository postRepository;
    private final LikeService likeService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public List<CommentResponseDto> getCommentsByTargetId(UUID targetId, Comment.TargetType type) {
        List<Comment> roots = commentRepository
                .findByTargetTypeAndTargetIdAndParentCommentIdIsNullOrderByCreatedAtDesc(type, targetId);

        return roots.stream()
                .map(this::toResponseWithReplies)
                .collect(Collectors.toList());
    }

    @Override
    public CommentResponseDto getCommentById(UUID id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        return toResponseWithReplies(comment);
    }

    @Override
    @Transactional
    public CommentResponseDto createComment(CommentRequestDto request) {
        Comment comment = commentMapper.toEntity(request);

        if (request.getParentCommentId() != null) {
            Comment parent = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new RuntimeException("Parent comment not found"));
            comment.setParentCommentId(parent.getId());
            comment.setTargetId(parent.getTargetId());
            comment.setTargetType(parent.getTargetType());
        } else {
            if (request.getTargetId() == null || request.getTargetType() == null) {
                throw new RuntimeException("targetId and targetType are required for root comments");
            }
            comment.setTargetId(request.getTargetId());
            comment.setTargetType(request.getTargetType());
        }

        AppUser user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        comment.setUser(user);
        Comment saved = commentRepository.save(comment);

        if (saved.getTargetType() == Comment.TargetType.POST) {
            postRepository.findById(saved.getTargetId()).ifPresent(post -> {
                Integer total = commentRepository.countByTargetTypeAndTargetId(Comment.TargetType.POST, post.getId());
                post.setCommentCount(total);
                postRepository.save(post);
            });
        }

        return toResponseWithReplies(saved);
    }

    @Override
    @Transactional
    public CommentResponseDto updateComment(UUID id, CommentRequestDto request) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (request.getContent() != null) {
            comment.setContent(request.getContent());
        }

        Comment updated = commentRepository.save(comment);
        return toResponseWithReplies(updated);
    }

    @Override
    @Transactional
    public void deleteComment(UUID id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        deleteRecursively(comment.getId());

        if (comment.getTargetType() == Comment.TargetType.POST) {
            postRepository.findById(comment.getTargetId()).ifPresent(post -> {
                Integer total = commentRepository.countByTargetTypeAndTargetId(Comment.TargetType.POST, post.getId());
                post.setCommentCount(total);
                postRepository.save(post);
            });
        }
    }

    @Override
    public List<CommentResponseDto> getReplies(UUID parentId) {
        List<Comment> replies = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(parentId);
        return replies.stream()
                .map(this::toResponseBasic)
                .collect(Collectors.toList());
    }

    private CommentResponseDto toResponseWithReplies(Comment comment) {
        CommentResponseDto dto = toResponseBasic(comment);

        List<Comment> replies = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(comment.getId());
        dto.setReplyCount(replies.size());
        dto.setReplies(
                replies.stream()
                        .map(this::toResponseBasic) // reply cũng có userDto
                        .collect(Collectors.toList()));

        return dto;
    }

    private CommentResponseDto toResponseBasic(Comment comment) {
        CommentResponseDto dto = commentMapper.toResponseDto(comment);

        if (comment.getUser() != null) {
            dto.setUser(userMapper.toBasicDto(comment.getUser()));
        }

        dto.setReplyCount(0);
        dto.setReplies(Collections.emptyList());

        try {
            Integer likes = likeService.getLikeInfo(comment.getId(), Like.TargetType.COMMENT, null).getLikeCount();
            dto.setLikeCount(likes);
        } catch (Exception ex) {

            dto.setLikeCount(Optional.ofNullable(comment.getLikeCount()).orElse(0));
        }

        return dto;
    }

    private void deleteRecursively(UUID commentId) {
        List<Comment> children = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(commentId);
        for (Comment child : children) {
            deleteRecursively(child.getId());
        }
        commentRepository.deleteById(commentId);
    }
}
