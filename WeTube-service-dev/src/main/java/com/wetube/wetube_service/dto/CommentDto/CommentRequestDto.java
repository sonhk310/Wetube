package com.wetube.wetube_service.dto.CommentDto;

import com.wetube.wetube_service.entity.interaction.Comment;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentRequestDto {
    private UUID targetId;
    private UUID userId;
    private String content;
    private UUID parentCommentId;
    private Comment.TargetType targetType;
}