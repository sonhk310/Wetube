package com.wetube.wetube_service.service.video;

import java.util.List;
import java.util.UUID;

import com.wetube.wetube_service.dto.request.CreateSubtitleRequest;
import com.wetube.wetube_service.dto.video.SubtitleCue;
import com.wetube.wetube_service.dto.video.SubtitleManageDto;
import com.wetube.wetube_service.dto.video.VideoSubtitleDto;

public interface VideoSubtitleService {
    VideoSubtitleDto addSubtitle(UUID videoId, CreateSubtitleRequest req);

    /** Phụ đề người xem thấy (đã công khai). */
    List<VideoSubtitleDto> getSubtitlesByVideo(UUID videoId);

    // Chủ video quản lý phụ đề (E2): kiểm tra quyền ở controller

    List<SubtitleManageDto> list(UUID videoId);

    SubtitleManageDto get(UUID videoId, UUID subtitleId);

    /** Tạo dòng PROCESSING cho lượt AI mới, thay các phụ đề cùng ngôn ngữ đang có. */
    SubtitleManageDto startAi(UUID videoId, String language);

    void completeAi(UUID subtitleId, List<SubtitleCue> cues);

    void failAi(UUID subtitleId, String message);

    SubtitleManageDto saveCues(UUID videoId, UUID subtitleId, List<SubtitleCue> cues);

    SubtitleManageDto publish(UUID videoId, UUID subtitleId);

    SubtitleManageDto unpublish(UUID videoId, UUID subtitleId);

    void delete(UUID videoId, UUID subtitleId);
}
