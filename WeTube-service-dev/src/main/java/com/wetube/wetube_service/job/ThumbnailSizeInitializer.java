package com.wetube.wetube_service.job;

import com.wetube.wetube_service.service.CloudinaryService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ảnh thu nhỏ video cũ nằm trong DB dưới dạng link đã cắt sẵn 300x200 (video ngang) hoặc 360x640 (Shorts), nên
 * bị vỡ nét ở thẻ rộng. Ảnh gốc vẫn còn trên Cloudinary, chỉ cần đổi phần biến đổi trong link sang kích thước mới
 * (xem {@link CloudinaryService#thumbnailUrl}). Idempotent; chạy trước khi đồng bộ Elasticsearch
 * ({@code ElasticBootstrapLoader}) để chỉ mục cũng có link mới, và không phụ thuộc thứ tự với
 * {@link ShortsThumbnailInitializer} vì xử lý cả hai kích thước cũ của Shorts.
 */
@Slf4j
@Component
@Order(1)
public class ThumbnailSizeInitializer implements CommandLineRunner {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            int regular = swap(false, CloudinaryService.THUMB_TRANSFORM, "c_fill,h_200,w_300");
            int shorts = swap(true, CloudinaryService.SHORT_THUMB_TRANSFORM, "c_fill,h_200,w_300", "c_fill,h_640,w_360");
            if (regular + shorts > 0) {
                log.info("Đã đổi ảnh thu nhỏ sang kích thước lớn: {} video ngang, {} Short", regular, shorts);
            }
        } catch (Exception e) {
            log.error("Không đổi được kích thước ảnh thu nhỏ: {}", e.getMessage());
        }
    }

    private int swap(boolean shorts, String newTransform, String... oldTransforms) {
        int total = 0;
        for (String old : oldTransforms) {
            total += em.createNativeQuery("""
                    UPDATE videos
                    SET thumbnail_url = REPLACE(thumbnail_url, :old, :new)
                    WHERE is_short = :shorts AND thumbnail_url LIKE :like
                    """)
                    .setParameter("old", "/" + old + "/")
                    .setParameter("new", "/" + newTransform + "/")
                    .setParameter("shorts", shorts ? 1 : 0)
                    .setParameter("like", "%/" + old + "/%")
                    .executeUpdate();
        }
        return total;
    }
}
