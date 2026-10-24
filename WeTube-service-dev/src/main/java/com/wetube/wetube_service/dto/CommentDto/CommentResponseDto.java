package com.wetube.wetube_service.dto.CommentDto;

import com.wetube.wetube_service.dto.UserDto;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponseDto {
    private UUID id;
    private String content;
    private UserDto user;
    private Integer likeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer replyCount;
    /** Chủ video đã ghim. */
    private Boolean pinned;
    /** Chủ video đã thả tim. */
    private Boolean hearted;
    /** Người viết là chủ video. */
    private Boolean byCreator;
    private List<CommentResponseDto> replies;
}
