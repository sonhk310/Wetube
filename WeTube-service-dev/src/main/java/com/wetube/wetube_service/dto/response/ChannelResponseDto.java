package com.wetube.wetube_service.dto.response;

import com.wetube.wetube_service.enumeration.ActiveStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class
ChannelResponseDto {
    private UUID id;
    private String name;
    private String picture;
    private String backgroundImgUrl;
    private String description;
    private String countryCode;
    private ActiveStatus status;
    private int totalSubscribers;
    private int totalVideos;
    private int totalViews;
    private LocalDateTime createdAt;

    private List<MemberTierResponseDto> membershipTiers;
}
