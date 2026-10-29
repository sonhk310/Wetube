package com.wetube.wetube_service.dto.membership;

import java.util.UUID;

/** Cấp hội viên trả phí của một kênh, như người xem thấy. */
public record TierDto(UUID id, String title, float price, String description) {}
