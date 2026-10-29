package com.wetube.wetube_service.dto.membership;

import java.time.LocalDate;
import java.util.UUID;

/** Hội viên hiện tại của người xem ở kênh: cấp và ngày hết hạn (hết ngày này). */
public record MyMembershipDto(UUID tierId, String tierTitle, LocalDate until) {}
