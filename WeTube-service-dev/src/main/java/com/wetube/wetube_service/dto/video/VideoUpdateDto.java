package com.wetube.wetube_service.dto.video;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VideoUpdateDto {
    private String title;
    private String description;
    private String status;
    private String tags;
}
