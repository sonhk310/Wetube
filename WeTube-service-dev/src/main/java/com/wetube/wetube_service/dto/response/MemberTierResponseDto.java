package com.wetube.wetube_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberTierResponseDto {
    private UUID id;
    private String title;
    private float price;
    @JsonProperty("isDefault")
    private boolean defaultTier;
    private String description;
}
