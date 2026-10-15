package com.wetube.wetube_service.dto.request;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubPackRequestDto {
    private String name;
    private Long price;
    private Integer durationDays;
    private String description;
}
