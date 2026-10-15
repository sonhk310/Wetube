package com.wetube.wetube_service.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserChannelResponseDto {
    private UUID id;
    private String backgroundImgUrl;
    private String name;
    private int totalSubscribers;
    private int totalVideos;
    private String description;
    private String countryCode;
    private LocalDateTime createdAt;
    private int totalViews;
}
