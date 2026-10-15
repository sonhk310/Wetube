package com.wetube.wetube_service.dto;

import java.util.UUID;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubPacksDto {
    @Id
    private UUID id;
    private String name;
    private Long price;
    private Integer durationDays;
    private String description;
}
