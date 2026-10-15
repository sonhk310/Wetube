package com.wetube.wetube_service.dto.video;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoWithUserDto {
    private UUID id;
    private String title;
    private String description;
    private String videoUrl;
    private String thumbnailUrl;
    private LocalDateTime createdAt;

    private String userId;
    private String username;
    private String email;
    private String avatarUrl;
}

