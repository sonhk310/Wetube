package com.wetube.wetube_service.repository.interaction;

import com.wetube.wetube_service.entity.interaction.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    List<Comment> findByTargetTypeAndTargetIdAndParentCommentIdIsNullOrderByCreatedAtDesc(Comment.TargetType targetType, UUID targetId);

    Integer countByTargetTypeAndTargetId(Comment.TargetType targetType, UUID targetId);

    /** Số bình luận (gồm cả trả lời) của nhiều đối tượng một lần: mỗi dòng là [targetId, số lượng]. */
    @Query("SELECT c.targetId, COUNT(c) FROM Comment c WHERE c.targetType = :type AND c.targetId IN :ids GROUP BY c.targetId")
    List<Object[]> countByTargetIds(@Param("type") Comment.TargetType type, @Param("ids") Collection<UUID> ids);

    List<Comment> findByParentCommentIdOrderByCreatedAtAsc(UUID parentCommentId);

    Integer countByParentCommentId(UUID parentCommentId);

    List<Comment> findByTargetTypeAndTargetId(Comment.TargetType targetType, UUID targetId);

    // Cập nhật thẳng (không qua @PreUpdate) để ghim / thả tim không làm đổi updatedAt ("đã chỉnh sửa")
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Comment c SET c.pinned = false WHERE c.targetType = :type AND c.targetId = :targetId AND c.pinned = true")
    int unpinAll(@Param("type") Comment.TargetType type, @Param("targetId") UUID targetId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Comment c SET c.pinned = :pinned WHERE c.id = :id")
    int setPinned(@Param("id") UUID id, @Param("pinned") boolean pinned);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Comment c SET c.hearted = :hearted WHERE c.id = :id")
    int setHearted(@Param("id") UUID id, @Param("hearted") boolean hearted);

}


