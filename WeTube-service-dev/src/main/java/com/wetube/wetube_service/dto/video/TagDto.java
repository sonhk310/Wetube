package com.wetube.wetube_service.dto.video;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TagDto {
    private UUID id;
    private String name;
    private LocalDateTime createdAt;
    private Integer count;
}
