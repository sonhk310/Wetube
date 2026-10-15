package com.wetube.wetube_service.job;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.wetube.wetube_service.service.livekit.LivekitIngressService;
import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class LiveKitJob {
    private final LivekitIngressService ingressService;

    /**
     * Chạy mỗi ngày lúc 00:00 (giờ VN) để xoá Ingress INACTIVE
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void cleanInactiveIngressJob() {
        log.info("=== [JOB] Start deleteInactiveIngress ===");
        try {
            var result = ingressService.deleteInactiveIngress();
            log.info("=== [JOB] Done deleteInactiveIngress: {} inactive, {} deleted ===",
                    result.get("inactiveCount"), result.get("deletedCount"));
        } catch (IOException e) {
            log.error("=== [JOB] Error when deleting inactive ingress ===", e);
        }
    }
}
