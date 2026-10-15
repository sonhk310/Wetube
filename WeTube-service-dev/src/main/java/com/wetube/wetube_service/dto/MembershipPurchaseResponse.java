package com.wetube.wetube_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MembershipPurchaseResponse {
    private UUID transactionId;

    private UUID userId;
    private String userName;  
    private String userEmail;  

    private UUID channelId;    
    private String channelName;

    private UUID membershipId;
    private String membershipTitle;
    private BigDecimal membershipPrice;

    private BigDecimal channelRevenue;
    private BigDecimal wetubeFee;

    private String createdAt; 
}
