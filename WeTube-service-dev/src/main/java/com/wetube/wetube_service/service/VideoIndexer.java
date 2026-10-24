package com.wetube.wetube_service.service;

import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.repository.video.VideoSearchRepository;
import com.wetube.wetube_service.search.VideoDocument;
import com.wetube.wetube_service.security.VideoAccess;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Nơi duy nhất ghi vào chỉ mục Elasticsearch "videos". Chỉ video đang được liệt kê (ACTIVE/PUBLIC) nằm trong
 * chỉ mục; mọi trạng thái khác (riêng tư, không công khai, bị ẩn, đã xoá) bị gỡ ra, nên tìm kiếm và gợi ý
 * không bao giờ trả về chúng. Lỗi Elasticsearch chỉ được ghi log, không làm hỏng thao tác chính.
 * Gọi {@link #index(Video)} khi video đang nằm trong transaction (cần đọc tag).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoIndexer {

    private static final IndexCoordinates INDEX = IndexCoordinates.of("videos");

    private final VideoSearchRepository searchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public VideoDocument toDocument(Video v) {
        VideoDocument doc = new VideoDocument();
        doc.setId(v.getId().toString());
        doc.setTitle(v.getTitle());
        doc.setDescription(v.getDescription());
        doc.setUserId(v.getUser().getId().toString());
        doc.setChannelId(v.getUser().getChannel() != null ? v.getUser().getChannel().getId().toString() : null);
        doc.setTags(v.getVideoTags().stream().map(vt -> vt.getTag().getName()).toList());
        doc.setCategories(List.of());
        doc.setCreatedAt(v.getCreatedAt().atZone(ZoneOffset.UTC).toInstant());
        doc.setThumbnailUrl(v.getThumbnailUrl());
        doc.setVideoUrl(v.getVideoUrl());
        doc.setTotalView(v.getTotalView());
        doc.setDuration(v.getDuration());
        doc.setIsShort(v.isShort());
        doc.setName(v.getUser().getName());
        doc.setPicture(v.getUser().getPicture());
        return doc;
    }

    /** Đưa video vào chỉ mục nếu đang được liệt kê, ngược lại gỡ khỏi chỉ mục. */
    public void index(Video video) {
        try {
            if (VideoAccess.isListed(video.getVideosStatus())) {
                searchRepository.save(toDocument(video));
                log.info("Indexed video {} to Elasticsearch", video.getId());
            } else {
                searchRepository.deleteById(video.getId().toString());
                log.info("Removed non-listed video {} ({}) from Elasticsearch", video.getId(), video.getVideosStatus());
            }
        } catch (Exception e) {
            log.error("Failed to sync video {} to Elasticsearch: {}", video.getId(), e.getMessage());
        }
    }

    public void remove(UUID videoId) {
        try {
            searchRepository.deleteById(videoId.toString());
        } catch (Exception e) {
            log.warn("Không gỡ được video {} khỏi Elasticsearch: {}", videoId, e.getMessage());
        }
    }

    /** Cập nhật riêng số lượt xem (video không nằm trong chỉ mục thì bỏ qua). */
    public void updateTotalView(UUID videoId, int totalView) {
        try {
            UpdateQuery query = UpdateQuery.builder(videoId.toString())
                    .withDocument(Document.from(Map.of("totalView", totalView)))
                    .build();
            elasticsearchOperations.update(query, INDEX);
        } catch (Exception e) {
            log.debug("Không cập nhật được lượt xem của video {} trong Elasticsearch: {}", videoId, e.getMessage());
        }
    }

    /** Đồng bộ lại toàn bộ: lưu video đang được liệt kê, gỡ phần còn lại. Trả về số video được index. */
    public int reindexAll(List<Video> videos) {
        int indexed = 0;
        for (Video v : videos) {
            if (VideoAccess.isListed(v.getVideosStatus())) {
                indexed++;
            }
            index(v);
        }
        return indexed;
    }
}
