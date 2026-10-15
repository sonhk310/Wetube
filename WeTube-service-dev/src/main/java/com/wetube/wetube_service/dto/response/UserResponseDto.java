package com.wetube.wetube_service.dto.response;

import com.wetube.wetube_service.enumeration.PlaylistType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {
    private String email;
    private String name;
    private String picture;
    private LocalDateTime createdAt;
    private UserChannelResponseDto channel;
}
