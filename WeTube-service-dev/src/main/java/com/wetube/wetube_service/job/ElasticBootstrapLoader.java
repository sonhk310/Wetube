package com.wetube.wetube_service.job;

import com.wetube.wetube_service.repository.channel.ChannelRepository;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.search.SearchIndexes;
import com.wetube.wetube_service.service.VideoIndexer;
import com.wetube.wetube_service.service.channel.ChannelIndexer;
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
    private final SearchIndexes searchIndexes;
    private final ChannelRepository channelRepository;
    private final ChannelIndexer channelIndexer;

    public ElasticBootstrapLoader(VideoRepository videoRepository, VideoIndexer videoIndexer,
            SearchIndexes searchIndexes, ChannelRepository channelRepository, ChannelIndexer channelIndexer) {
        this.videoRepository = videoRepository;
        this.videoIndexer = videoIndexer;
        this.searchIndexes = searchIndexes;
        this.channelRepository = channelRepository;
        this.channelIndexer = channelIndexer;
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
        // Tạo videos_v2 / channels nếu chưa có (không được thì dùng chỉ mục cũ "videos", kênh tìm bằng MySQL)
        searchIndexes.ensureIndexes();
        var videos = videoRepository.findAll();
        int indexed = videoIndexer.reindexAll(videos);
        log.info("✅ Sync xong: {} video được index, {} video không công khai bị gỡ khỏi chỉ mục",
                indexed, videos.size() - indexed);
        if (searchIndexes.channelsReady()) {
            var channels = new java.util.ArrayList<com.wetube.wetube_service.entity.channel.Channel>();
            channelRepository.findAll().forEach(channels::add);
            channelIndexer.reindexAll(channels);
            log.info("✅ Sync kênh xong: {} kênh", channels.size());
        }
    }
}
