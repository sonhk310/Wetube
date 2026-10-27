package com.wetube.wetube_service.service.interaction.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.wetube.wetube_service.enumeration.InteractionType;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.service.video.TagAffinityService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserInteractionServiceImpl {

    private final VideoRepository videoRepo;
    private final TagAffinityService tagAffinityService;

    /**
     * Chỉ SHARE do client báo (bấm sao chép / chia sẻ). Xem, thích, bình luận được backend tự cộng điểm lúc xảy ra
     * (lượt xem đã chống trùng, thích / bình luận là thao tác thật), nên VIEW / LIKE / COMMENT gửi lên đây bị bỏ qua:
     * client bản cũ vẫn gửi VIEW khi bấm thẻ video, không được cộng hai lần.
     */
    public void saveInteraction(UUID userId, UUID videoId, InteractionType type) {
        videoRepo.findById(videoId).orElseThrow(() ->
                new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Video not found"));

        if (type == InteractionType.SHARE) {
            tagAffinityService.addQuietly(userId, videoId, TagAffinityService.SHARE);
        }
    }
}
