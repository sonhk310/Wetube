package com.wetube.wetube_service.job;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.wetube.wetube_service.service.live.LiveStreamService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Nhịp theo dõi các buổi phát trực tiếp và dọn Ingress mồ côi. */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiveStatusJob {

    private final LiveStreamService service;

    /** Mỗi 15 giây: hỏi LiveKit để chuyển trạng thái, cập nhật số người xem, tự kết thúc buổi quá hạn. */
    @Scheduled(fixedDelay = 15_000)
    public void tick() {
        try {
            service.refreshAll(LocalDateTime.now());
        } catch (RuntimeException e) {
            log.warn("[Live] Job theo dõi buổi phát lỗi: {}", e.getMessage());
        }
    }

    /** 00:00 giờ VN: xoá Ingress không hoạt động (trừ của các buổi chưa kết thúc). */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void cleanIngress() {
        int deleted = service.cleanInactiveIngress();
        log.info("[Live] Đã xoá {} Ingress không dùng", deleted);
    }
}
