package com.wetube.wetube_service.service.interaction;

import com.wetube.wetube_service.dto.LikeDto;
import com.wetube.wetube_service.entity.interaction.Like;

import java.util.UUID;

public interface LikeService {
    LikeDto getLikeInfo(UUID targetId, Like.TargetType targetType, UUID userId);
    void toggleLike(UUID targetId, Like.TargetType targetType, UUID userId);

    /** Bật/tắt "không thích" video; thích video sẽ bỏ không thích và ngược lại. Trả về trạng thái mới của người dùng. */
    LikeDto toggleDislike(UUID videoId, UUID userId);
}

