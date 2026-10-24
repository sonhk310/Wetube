package com.wetube.wetube_service.service.interaction.impl;

import com.wetube.wetube_service.exception.ResourceNotFoundException;
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
import com.wetube.wetube_service.entity.notification.Notification;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
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
    private final VideoRepository videoRepository;
    private final NotificationService notificationService;

    @Override
    public List<CommentResponseDto> getCommentsByTargetId(UUID targetId, Comment.TargetType type, Sort sort) {
        List<Comment> roots = commentRepository
                .findByTargetTypeAndTargetIdAndParentCommentIdIsNullOrderByCreatedAtDesc(type, targetId);
        UUID ownerId = type == Comment.TargetType.VIDEO ? videoOwnerId(targetId) : null;

        // Danh sách đã theo mới nhất; sort ổn định nên cùng điểm thì bình luận mới hơn vẫn đứng trước
        Comparator<CommentResponseDto> order = Comparator.comparing(
                (CommentResponseDto c) -> Boolean.TRUE.equals(c.getPinned())).reversed();
        if (sort == Sort.TOP) {
            order = order.thenComparing(
                    c -> Optional.ofNullable(c.getLikeCount()).orElse(0) + Optional.ofNullable(c.getReplyCount()).orElse(0),
                    Comparator.reverseOrder());
        }
        return roots.stream()
                .map(c -> toResponseWithReplies(c, ownerId))
                .sorted(order)
                .collect(Collectors.toList());
    }

    @Override
    public CommentResponseDto getCommentById(UUID id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        return toResponseWithReplies(comment, ownerOf(comment));
    }

    @Override
    @Transactional(readOnly = true)
    public UUID videoOwnerOf(UUID commentId) {
        return commentRepository.findById(commentId).map(this::ownerOf).orElse(null);
    }

    @Override
    @Transactional
    public CommentResponseDto setPinned(UUID commentId, boolean pinned) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        if (comment.getParentCommentId() != null || comment.getTargetType() != Comment.TargetType.VIDEO) {
            throw new IllegalArgumentException("Chỉ ghim được bình luận gốc dưới video");
        }
        if (pinned) {
            commentRepository.unpinAll(comment.getTargetType(), comment.getTargetId());
        }
        commentRepository.setPinned(commentId, pinned);
        return getCommentById(commentId);
    }

    @Override
    @Transactional
    public CommentResponseDto setHearted(UUID commentId, boolean hearted, UUID actorId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        boolean wasHearted = Boolean.TRUE.equals(comment.getHearted());
        UUID authorId = comment.getUser() != null ? comment.getUser().getId() : null;
        UUID videoId = comment.getTargetId();
        String content = comment.getContent();
        commentRepository.setHearted(commentId, hearted);
        if (hearted && !wasHearted) {
            try {
                notificationService.notifyCommentHeart(authorId, actorId, videoId, commentId, content);
            } catch (Exception e) {
                log.warn("Không tạo được thông báo tim cho bình luận {}: {}", commentId, e.getMessage());
            }
        }
        Comment updated = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        return toResponseBasic(updated, ownerOf(updated));
    }

    @Override
    @Transactional
    public CommentResponseDto createComment(CommentRequestDto request) {
        Comment comment = commentMapper.toEntity(request);

        if (request.getParentCommentId() != null) {
            Comment parent = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found"));
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
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        comment.setUser(user);
        Comment saved = commentRepository.save(comment);
        notifyAboutComment(saved, user.getId());
        UUID ownerId = ownerOf(saved);

        if (saved.getTargetType() == Comment.TargetType.POST) {
            postRepository.findById(saved.getTargetId()).ifPresent(post -> {
                Integer total = commentRepository.countByTargetTypeAndTargetId(Comment.TargetType.POST, post.getId());
                post.setCommentCount(total);
                postRepository.save(post);
            });
        }

        return toResponseWithReplies(saved, ownerId);
    }

    /** Trả lời bình luận -> báo người viết bình luận gốc; bình luận mới trên video -> báo chủ video. */
    private void notifyAboutComment(Comment saved, UUID authorId) {
        if (saved.getTargetType() != Comment.TargetType.VIDEO) {
            return;
        }
        try {
            UUID videoId = saved.getTargetId();
            if (saved.getParentCommentId() != null) {
                UUID parentAuthor = commentRepository.findById(saved.getParentCommentId())
                        .map(c -> c.getUser() != null ? c.getUser().getId() : null)
                        .orElse(null);
                notificationService.notifyComment(Notification.Type.COMMENT_REPLY, parentAuthor, authorId,
                        videoId, saved.getId(), saved.getContent());
            } else {
                UUID owner = videoRepository.findById(videoId).map(v -> v.getUser().getId()).orElse(null);
                notificationService.notifyComment(Notification.Type.VIDEO_COMMENT, owner, authorId,
                        videoId, saved.getId(), saved.getContent());
            }
        } catch (Exception e) {
            log.warn("Không tạo được thông báo cho bình luận {}: {}", saved.getId(), e.getMessage());
        }
    }

    @Override
    @Transactional
    public CommentResponseDto updateComment(UUID id, CommentRequestDto request) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        if (request.getContent() != null) {
            comment.setContent(request.getContent());
        }

        Comment updated = commentRepository.save(comment);
        return toResponseWithReplies(updated, ownerOf(updated));
    }

    @Override
    @Transactional
    public void deleteComment(UUID id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

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
        UUID ownerId = replies.isEmpty() ? null : ownerOf(replies.get(0));
        return replies.stream()
                .map(c -> toResponseBasic(c, ownerId))
                .collect(Collectors.toList());
    }

    /** Chủ video chứa bình luận, null nếu không phải bình luận video. */
    private UUID ownerOf(Comment comment) {
        return comment.getTargetType() == Comment.TargetType.VIDEO ? videoOwnerId(comment.getTargetId()) : null;
    }

    private UUID videoOwnerId(UUID videoId) {
        return videoRepository.findById(videoId)
                .map(v -> v.getUser() != null ? v.getUser().getId() : null)
                .orElse(null);
    }

    private CommentResponseDto toResponseWithReplies(Comment comment, UUID videoOwnerId) {
        CommentResponseDto dto = toResponseBasic(comment, videoOwnerId);

        List<Comment> replies = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(comment.getId());
        dto.setReplyCount(replies.size());
        dto.setReplies(
                replies.stream()
                        .map(c -> toResponseBasic(c, videoOwnerId)) // reply cũng có userDto
                        .collect(Collectors.toList()));

        return dto;
    }

    private CommentResponseDto toResponseBasic(Comment comment, UUID videoOwnerId) {
        CommentResponseDto dto = commentMapper.toResponseDto(comment);

        if (comment.getUser() != null) {
            dto.setUser(userMapper.toBasicDto(comment.getUser()));
        }
        dto.setPinned(Boolean.TRUE.equals(comment.getPinned()));
        dto.setHearted(Boolean.TRUE.equals(comment.getHearted()));
        dto.setByCreator(videoOwnerId != null && comment.getUser() != null
                && videoOwnerId.equals(comment.getUser().getId()));

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
