package com.wetube.wetube_service.dto.response;

import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.enumeration.SubscriptionType;
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
    private UUID userId;
    private String name;
    private String handle;
    private String picture;
    private String backgroundImgUrl;
    private String description;
    private String countryCode;
    private ActiveStatus status;
    private int totalSubscribers;
    private int totalVideos;
    private int totalViews;
    private LocalDateTime createdAt;

    /** Người xem hiện tại đã đăng ký kênh này chưa (false nếu chưa đăng nhập). */
    private boolean subscribed;
    /** Chế độ thông báo của người xem với kênh này; null nếu chưa đăng ký. */
    private SubscriptionType notificationMode;

    private List<MemberTierResponseDto> membershipTiers;
}
