package com.wetube.wetube_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentResponseDto {

    private String payUrl;
    private String deeplink;
    private String qrCodeUrl;
    private String orderId;
    private String requestId;
    private Long amount;
    private String message;
    private Integer resultCode;
    
}
