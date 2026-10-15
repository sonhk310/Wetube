package com.wetube.wetube_service.dto.video;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoTagDto {
    private UUID videoId;
    private UUID tagId;

    private String tagName;
    private String videoTitle;
}
