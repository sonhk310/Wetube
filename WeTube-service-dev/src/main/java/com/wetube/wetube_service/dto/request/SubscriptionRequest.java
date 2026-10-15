package com.wetube.wetube_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionRequest {
    private UUID subscriberId;
    private UUID channelId;
    private UUID tierId;   
}
