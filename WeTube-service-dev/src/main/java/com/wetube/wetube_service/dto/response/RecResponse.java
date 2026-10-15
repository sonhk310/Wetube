package com.wetube.wetube_service.dto.response;

import com.wetube.wetube_service.dto.video.VideoDto;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RecResponse {
    private String message;
    List<VideoDto> videos;
}
