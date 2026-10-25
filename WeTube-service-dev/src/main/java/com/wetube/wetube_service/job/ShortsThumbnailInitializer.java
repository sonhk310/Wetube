package com.wetube.wetube_service.job;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ảnh thu nhỏ của Shorts trước đây bị cắt thành khung ngang 300x200. Ảnh gốc vẫn còn trên Cloudinary,
 * nên chỉ cần đổi phần biến đổi trong link sang khung dọc 360x640 (như {@code CloudinaryService.thumbnailUrl}).
 * Idempotent; chạy trước khi đồng bộ Elasticsearch ({@code ElasticBootstrapLoader}) để chỉ mục có link mới.
 */
@Slf4j
@Component
@Order(1)
public class ShortsThumbnailInitializer implements CommandLineRunner {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            int updated = em.createNativeQuery("""
                    UPDATE videos
                    SET thumbnail_url = REPLACE(thumbnail_url, '/c_fill,h_200,w_300/', '/c_fill,h_640,w_360/')
                    WHERE is_short = 1 AND thumbnail_url LIKE '%/c_fill,h_200,w_300/%'
                    """).executeUpdate();
            if (updated > 0) {
                log.info("Đã đổi ảnh thu nhỏ của {} Short sang khung dọc 360x640", updated);
            }
        } catch (Exception e) {
            log.error("Không đổi được ảnh thu nhỏ của Shorts: {}", e.getMessage());
        }
    }
}
