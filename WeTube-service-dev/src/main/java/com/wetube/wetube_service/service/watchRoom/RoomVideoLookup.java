package com.wetube.wetube_service.service.watchRoom;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.wetube.wetube_service.dto.room.VideoRoom;
import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.mapper.video.VideoMapper;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.security.VideoAccess;

import lombok.RequiredArgsConstructor;

/** Đọc video cho phòng xem chung (cần transaction vì mapper đọc kênh của video). */
@Component
@RequiredArgsConstructor
public class RoomVideoLookup {

    private final VideoRepository videoRepo;
    private final VideoMapper videoMapper;

    /** Video công khai hoặc không công khai; null nếu id sai, không có, riêng tư, bị ẩn hoặc đã xoá. */
    @Transactional(readOnly = true)
    public VideoRoom find(String videoId) {
        if (videoId == null || videoId.isBlank()) {
            return null;
        }
        UUID id;
        try {
            id = UUID.fromString(videoId);
        } catch (IllegalArgumentException e) {
            return null;
        }
        Video video = videoRepo.findByIdWithTags(id).orElse(null);
        if (video == null) {
            return null;
        }
        ActiveStatus status = video.getVideosStatus();
        if (!VideoAccess.isListed(status) && status != ActiveStatus.UNLISTED) {
            return null;
        }
        return videoMapper.toRoomDto(video);
    }
}
