package com.wetube.wetube_service.dto;

import com.wetube.wetube_service.entity.interaction.Like;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LikeDto {
    private UUID targetId;
    private Like.TargetType targetType;
    private List<UUID> likedUserIds;
    private Integer likeCount;
    private Boolean liked;
}

