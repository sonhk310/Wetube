package com.wetube.wetube_service.service.video;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.wetube.wetube_service.entity.video.UserTag;
import com.wetube.wetube_service.entity.video.UserTopic;
import com.wetube.wetube_service.entity.video.VideoTag;
import com.wetube.wetube_service.repository.video.UserTagRepository;
import com.wetube.wetube_service.repository.video.UserTopicRepository;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.repository.video.VideoTagRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Điểm yêu thích thẻ của người dùng (bảng user_tags), nguồn của gợi ý trang chủ. Cộng điểm cho mọi thẻ của video
 * khi người dùng xem / thích / bình luận / chia sẻ video đó. Gọi từ controller (ngoài transaction của thao tác
 * chính); lỗi chỉ ghi log, không làm hỏng thao tác chính.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagAffinityService {

    public static final double VIEW = 9.0;
    public static final double LIKE = 5.0;
    public static final double COMMENT = 8.0;
    public static final double SHARE = 12.0;

    /** Điểm một thẻ không vượt quá mức này. */
    static final double MAX_POINT = 1000;

    private final VideoTagRepository videoTagRepo;
    private final UserTagRepository userTagRepo;
    private final VideoRepository videoRepo;
    private final UserTopicRepository userTopicRepo;
    private final TopicCatalog topicCatalog;

    /** Cộng điểm cho mọi thẻ của video và cho chủ đề của video (kèm nhóm của nó, C8). */
    public void addQuietly(UUID userId, UUID videoId, double delta) {
        try {
            for (VideoTag vt : videoTagRepo.findByVideo_Id(videoId)) {
                addPoint(userId, vt.getTag().getId(), delta);
            }
        } catch (Exception e) {
            log.warn("Không cộng được điểm thẻ cho người dùng {} / video {}: {}", userId, videoId, e.getMessage());
        }
        try {
            for (String topicId : topicCatalog.withGroup(videoRepo.findTopicIdById(videoId))) {
                addTopicPoint(userId, topicId, delta);
            }
        } catch (Exception e) {
            log.warn("Không cộng được điểm chủ đề cho người dùng {} / video {}: {}", userId, videoId, e.getMessage());
        }
    }

    private void addTopicPoint(UUID userId, String topicId, double delta) {
        var pk = new UserTopic.UserTopicId(userId, topicId);
        var existing = userTopicRepo.findById(pk).orElse(null);
        if (existing == null) {
            try {
                userTopicRepo.save(UserTopic.builder().userId(userId).topicId(topicId).point(delta).build());
                return;
            } catch (DataIntegrityViolationException e) {
                existing = userTopicRepo.findById(pk).orElseThrow();
            }
        }
        existing.setPoint(Math.min(existing.getPoint() + delta, MAX_POINT));
        userTopicRepo.save(existing);
    }

    private void addPoint(UUID userId, UUID tagId, double delta) {
        var pk = new UserTag.UserTagId(userId, tagId);
        var existing = userTagRepo.findById(pk).orElse(null);
        if (existing == null) {
            try {
                userTagRepo.save(UserTag.builder().userId(userId).tagId(tagId).point(delta).build());
                return;
            } catch (DataIntegrityViolationException e) {
                // Hai yêu cầu cùng tạo dòng đầu tiên: dòng kia đã có, cộng vào đó
                existing = userTagRepo.findById(pk).orElseThrow();
            }
        }
        existing.setPoint(Math.min(existing.getPoint() + delta, MAX_POINT));
        userTagRepo.save(existing);
    }
}
