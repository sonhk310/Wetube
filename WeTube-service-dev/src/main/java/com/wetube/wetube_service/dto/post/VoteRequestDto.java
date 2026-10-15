package com.wetube.wetube_service.dto.post;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoteRequestDto {
    private UUID postId;
    private UUID optionId;
    private UUID userId;
}
