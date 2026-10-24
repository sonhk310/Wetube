package com.wetube.wetube_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnsubscribeRequest {
    private UUID subscriberId;
    private UUID tierId;
    /** Dùng khi không gửi tierId: huỷ đăng ký gói mặc định của kênh. */
    private UUID channelId;
}
