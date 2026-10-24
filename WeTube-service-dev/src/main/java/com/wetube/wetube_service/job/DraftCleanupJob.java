package com.wetube.wetube_service.job;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.service.video.VideoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DraftCleanupJob {
    static final int MAX_AGE_DAYS = 30;

    private final VideoRepository videoRepository;
    private final VideoService videoService;

    /**
     * Chạy mỗi ngày lúc 03:00 (giờ VN): xoá bản nháp không ai sửa quá 30 ngày (xoá cả file trên Cloudinary).
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Ho_Chi_Minh")
    public void deleteAbandonedDrafts() {
        var drafts = videoRepository.findByVideosStatusAndUpdatedAtBefore(
                ActiveStatus.DRAFT, LocalDateTime.now().minusDays(MAX_AGE_DAYS));
        int deleted = 0;
        for (Video draft : drafts) {
            try {
                videoService.deleteVideo(draft.getId());
                deleted++;
            } catch (Exception e) {
                log.warn("Không xoá được bản nháp {}: {}", draft.getId(), e.getMessage());
            }
        }
        log.info("=== [JOB] Dọn bản nháp: {}/{} đã xoá ===", deleted, drafts.size());
    }
}
