package com.wetube.wetube_service.dto.response;
import java.time.LocalDate;
import java.util.UUID;

import com.wetube.wetube_service.enumeration.ActiveStatus;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PremiumUserResponseDto {
    private UUID userId;
    private UUID SubPackId;
    private LocalDate startData;
    private LocalDate endDate;
    private ActiveStatus status;
}
