package com.wetube.wetube_service.dto;


import java.time.LocalDate;
import java.util.UUID;

import com.wetube.wetube_service.enumeration.ActiveStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PremiumUserDto {
    private UUID id;
    private UUID userId;
    private String username;
    private UUID subPackId;
    private String subPackName;
    private LocalDate startDate;
    private LocalDate endDate;
    private ActiveStatus status;
}
