package com.wetube.wetube_service.dto.response;

import java.util.UUID;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMomoResponseDto {
    private UUID premiumUserId;
    private UUID planId;
    private String provider;
    private String eventType;
    private String payload;
    private Long amount;
}
