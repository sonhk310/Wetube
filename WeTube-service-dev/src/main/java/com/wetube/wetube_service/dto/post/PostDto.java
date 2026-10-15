package com.wetube.wetube_service.dto.post;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDto {
    private UUID id;
    private UUID userId;
    private String content;
    private String imageUrl;
    private Integer commentCount;
    private Integer likeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private PollSummaryDto poll;
}
