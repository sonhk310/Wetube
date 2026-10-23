package com.wetube.wetube_service.job;

import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.service.VideoIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ElasticBootstrapLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ElasticBootstrapLoader.class);

    private final VideoRepository videoRepository;
    private final VideoIndexer videoIndexer;

    public ElasticBootstrapLoader(VideoRepository videoRepository, VideoIndexer videoIndexer) {
        this.videoRepository = videoRepository;
        this.videoIndexer = videoIndexer;
    }

    @Override
    @Transactional(readOnly = true)
    public void run(String... args) {
        try {
            sync();
        } catch (Exception e) {
            // Không để lỗi đồng bộ Elasticsearch làm sập cả ứng dụng
            log.error("❌ Sync MySQL → Elasticsearch thất bại, bỏ qua và tiếp tục khởi động", e);
        }
    }

    /** Chỉ video đang được liệt kê nằm trong chỉ mục; video ẩn/riêng tư/đã xoá bị gỡ ra mỗi lần khởi động. */
    private void sync() {
        log.info("🚀 Bắt đầu sync MySQL → Elasticsearch...");
        var videos = videoRepository.findAll();
        int indexed = videoIndexer.reindexAll(videos);
        log.info("✅ Sync xong: {} video được index, {} video không công khai bị gỡ khỏi chỉ mục",
                indexed, videos.size() - indexed);
    }
}
