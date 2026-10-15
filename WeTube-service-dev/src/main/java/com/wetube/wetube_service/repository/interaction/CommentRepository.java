package com.wetube.wetube_service.repository.interaction;

import com.wetube.wetube_service.entity.interaction.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    List<Comment> findByTargetTypeAndTargetIdAndParentCommentIdIsNullOrderByCreatedAtDesc(Comment.TargetType targetType, UUID targetId);

    Integer countByTargetTypeAndTargetId(Comment.TargetType targetType, UUID targetId);

    List<Comment> findByParentCommentIdOrderByCreatedAtAsc(UUID parentCommentId);

    Integer countByParentCommentId(UUID parentCommentId);

    List<Comment> findByTargetTypeAndTargetId(Comment.TargetType targetType, UUID targetId);

}


