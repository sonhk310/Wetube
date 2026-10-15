package com.wetube.wetube_service.service.video;

import java.util.List;
import java.util.UUID;

import com.wetube.wetube_service.dto.request.CreateSubtitleRequest;
import com.wetube.wetube_service.dto.video.VideoSubtitleDto;

public interface VideoSubtitleService {
    VideoSubtitleDto addSubtitle(UUID videoId, CreateSubtitleRequest req);
    List<VideoSubtitleDto> getSubtitlesByVideo(UUID videoId);
}

