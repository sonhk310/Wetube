package com.wetube.wetube_service.dto.membership;

import java.util.List;

/** Các cấp trả phí đang nhận hội viên (giá thấp trước) và hội viên hiện tại của người xem (null nếu không có). */
public record ChannelTiersDto(List<TierDto> tiers, MyMembershipDto membership) {}
