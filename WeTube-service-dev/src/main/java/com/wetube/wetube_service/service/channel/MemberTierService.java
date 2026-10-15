package com.wetube.wetube_service.service.channel;

import com.wetube.wetube_service.dto.response.MemberTierResponseDto;

import java.util.UUID;

public interface MemberTierService {
    UUID addTier(UUID channelId, MemberTierResponseDto dto);
    void updateTier(UUID tierId, MemberTierResponseDto dto);
    void deleteTier(UUID tierId);
}
