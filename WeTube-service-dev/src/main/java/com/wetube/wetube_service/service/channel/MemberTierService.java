package com.wetube.wetube_service.service.channel;

import com.wetube.wetube_service.dto.response.MemberTierResponseDto;

import java.util.UUID;

public interface MemberTierService {
    UUID addTier(UUID channelId, MemberTierResponseDto dto);
    void updateTier(UUID tierId, MemberTierResponseDto dto);
    /** Trả true nếu cấp đã có người mua nên chỉ ngừng nhận hội viên mới (không xoá thật). */
    boolean deleteTier(UUID tierId);
}
