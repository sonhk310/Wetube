package com.wetube.wetube_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscribedChannelResponseDto {
    private UUID channelId;
    private String name;
    private String avatarUrl;
    private int subscribers;
    private String description;
    private String videoUrl;
}
