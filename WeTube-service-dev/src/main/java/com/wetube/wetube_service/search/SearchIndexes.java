package com.wetube.wetube_service.search;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Chỉ mục Elasticsearch do backend tự quản: videos_v2 và channels, cùng cách phân tích chữ ở
 * resources/elastic (bỏ dấu, mẩu 3 chữ cho chữ viết dính, tiền tố cho gợi ý). Tạo lúc khởi động nếu chưa có.
 * Không tạo được thì video quay về chỉ mục cũ "videos" (tạo tay trên server) và kênh tìm bằng MySQL.
 */
@Slf4j
@Component
public class SearchIndexes {
    static final IndexCoordinates OLD_VIDEOS = IndexCoordinates.of("videos");
    static final IndexCoordinates VIDEOS_V2 = IndexCoordinates.of("videos_v2");
    public static final IndexCoordinates CHANNELS = IndexCoordinates.of("channels");

    private final ElasticsearchOperations operations;
    private volatile IndexCoordinates videos = OLD_VIDEOS;
    private volatile boolean channelsReady = false;

    public SearchIndexes(ElasticsearchOperations operations) {
        this.operations = operations;
    }

    /** Chỉ mục video đang dùng. */
    public IndexCoordinates videos() {
        return videos;
    }

    /** Có chỉ mục kênh hay không (không có thì tìm kênh bằng MySQL). */
    public boolean channelsReady() {
        return channelsReady;
    }

    public void ensureIndexes() {
        if (ensure(VIDEOS_V2, "elastic/videos_v2-mapping.json")) {
            videos = VIDEOS_V2;
        }
        channelsReady = ensure(CHANNELS, "elastic/channels-mapping.json");
        log.info("Chỉ mục tìm kiếm: video = {}, kênh = {}", videos.getIndexName(),
                channelsReady ? CHANNELS.getIndexName() : "MySQL");
    }

    private boolean ensure(IndexCoordinates index, String mappingFile) {
        try {
            IndexOperations ops = operations.indexOps(index);
            if (ops.exists()) {
                return true;
            }
            Document settings = Document.parse(read("elastic/analysis.json"));
            ops.create(settings, Document.parse(read(mappingFile)));
            log.info("Đã tạo chỉ mục {}", index.getIndexName());
            return true;
        } catch (Exception e) {
            log.warn("Không tạo / dùng được chỉ mục {}: {}", index.getIndexName(), e.getMessage());
            return false;
        }
    }

    private static String read(String path) throws IOException {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
