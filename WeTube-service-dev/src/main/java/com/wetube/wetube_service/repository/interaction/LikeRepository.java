package com.wetube.wetube_service.repository.interaction;

import com.wetube.wetube_service.entity.interaction.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LikeRepository extends JpaRepository<Like, UUID> {

    Integer countByTargetTypeAndTargetId(Like.TargetType targetType, UUID targetId);

    List<Like> findByTargetTypeAndTargetId(Like.TargetType targetType, UUID targetId);

    Boolean existsByTargetTypeAndTargetIdAndUserId(Like.TargetType targetType, UUID targetId, UUID userId);
}
