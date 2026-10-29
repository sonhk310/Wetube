package com.wetube.wetube_service.dto.membership;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Studio của chủ kênh: các cấp (kể cả đã ngừng nhận), hội viên còn hạn và doanh thu hội viên. */
public record OwnerMembersDto(List<OwnerTier> tiers, List<MemberRow> members, Revenue revenue) {

    public record OwnerTier(UUID id, String title, float price, String description, boolean archived, int memberCount) {}

    public record MemberRow(UUID userId, String name, String picture, String handle, String tierTitle,
            LocalDate since, LocalDate until) {}

    /** total: tiền hội viên thu; wetubeFee: phần WeTube giữ (10%); channelRevenue: phần kênh nhận. Đơn vị VND. */
    public record Revenue(BigDecimal total, BigDecimal wetubeFee, BigDecimal channelRevenue) {}
}
