package com.wetube.wetube_service.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.stereotype.Service;

import com.wetube.wetube_service.dto.video.VideoDto;
import com.wetube.wetube_service.mapper.video.VideoMapper;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.search.SearchFilters;
import com.wetube.wetube_service.search.SearchIndexes;
import com.wetube.wetube_service.search.VideoDocument;
import com.wetube.wetube_service.security.VideoAccess;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoSearchService {
        private final ElasticsearchOperations elasticsearchOperations;
        private final VideoRepository videoRepository;
        private final VideoMapper videoMapper;
        private final SearchIndexes indexes;

        // ================== SEARCH TRẢ VỀ ELASTIC DOCUMENT ==================

        public Page<VideoDocument> searchByTitle(String text, int page, int size) {
                Instant start = Instant.now();
                Pageable pageable = PageRequest.of(page, size);

                NativeQuery query = new NativeQueryBuilder()
                                .withQuery(bool(b -> b
                                                .should(wildcard(w -> w.field("title").value(text.toLowerCase() + "*")))
                                                .should(multiMatch(m -> m
                                                                .fields("title", "description")
                                                                .query(text)
                                                                .fuzziness("AUTO")))))
                                .withPageable(pageable)
                                .build();

                Page<VideoDocument> results = search(query, pageable);

                log.info("[ELASTIC] Search by title='{}' found {} results in {}ms",
                                text, results.getTotalElements(), Duration.between(start, Instant.now()).toMillis());

                return results;
        }

        public VideoDocument searchExactTitle(String title) {
                NativeQuery query = new NativeQueryBuilder()
                                .withQuery(term(t -> t.field("title.keyword").value(title)))
                                .build();

                SearchHits<VideoDocument> hits = elasticsearchOperations.search(query, VideoDocument.class, indexes.videos());
                return hits.hasSearchHits() ? hits.getSearchHits().get(0).getContent() : null;
        }

        public List<String> suggestTitles(String prefix, int size) {
                // Khớp tiền tố tiêu đề ("vi" -> "view") được ưu tiên, vẫn giữ khớp gần đúng để chịu lỗi gõ
                NativeQuery query = new NativeQueryBuilder()
                                .withQuery(bool(b -> b
                                                .should(matchPhrasePrefix(m -> m.field("title").query(prefix).boost(3f)))
                                                // videos_v2: tiền tố không dấu ("lac tr" -> "Lạc trôi")
                                                .should(match(m -> m.field("title.prefix").query(prefix)
                                                                .operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.And)
                                                                .boost(3f)))
                                                .should(multiMatch(m -> m
                                                                .fields("title", "description", "tags", "name", "channelName")
                                                                .query(prefix)
                                                                .fuzziness("AUTO")))
                                                .minimumShouldMatch("1")))
                                .withSourceFilter(new FetchSourceFilter(true, new String[]{"title"}, new String[]{}))
                                .withMaxResults(size)
                                .build();

                SearchHits<VideoDocument> hits = elasticsearchOperations.search(query, VideoDocument.class, indexes.videos());

                return hits.getSearchHits().stream()
                                .map(h -> h.getContent().getTitle())
                                .filter(Objects::nonNull)
                                .distinct()
                                .toList();
        }

        // ================== SEARCH TRẢ VỀ VIDEO DTO (LOAD FULL TỪ MYSQL)
        // ==================

        public Page<VideoDto> searchByTitleFull(String text, SearchFilters filters, int page, int size) {
                Instant start = Instant.now();
                Pageable pageable = switch (filters.sort()) {
                        case DATE -> PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
                        case VIEWS -> PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "totalView"));
                        case RELEVANCE -> PageRequest.of(page, size);
                };

                NativeQuery query = new NativeQueryBuilder()
                                .withQuery(bool(b -> {
                                        // Khớp cả cụm xếp đầu; rồi khớp từ (không dấu, sai 1-2 ký tự) ở tiêu đề / kênh / thẻ / mô tả;
                                        // rồi khớp mẩu 3 chữ (viết dính, thiếu ký tự). Các trường mới chỉ có ở videos_v2.
                                        b.should(matchPhrase(m -> m.field("title").query(text).boost(6f)))
                                                        .should(multiMatch(m -> m
                                                                        .fields("title^3", "channelName^2", "tags.text^2", "description")
                                                                        .query(text)
                                                                        .fuzziness("AUTO")
                                                                        .boost(2f)))
                                                        .should(match(m -> m.field("title.compact").query(text)
                                                                        .minimumShouldMatch("60%")))
                                                        .should(match(m -> m.field("channelName.compact").query(text)
                                                                        .minimumShouldMatch("60%")))
                                                        .should(wildcard(w -> w.field("title").value(text.toLowerCase() + "*")))
                                                        // Có filter thì should thành tuỳ chọn: bắt buộc khớp ít nhất 1
                                                        .minimumShouldMatch("1");
                                        if (filters.shortsOnly() != null) {
                                                b.filter(term(t -> t.field("isShort").value(filters.shortsOnly())));
                                        }
                                        if (filters.minDurationSeconds() != null || filters.maxDurationSeconds() != null) {
                                                b.filter(range(r -> r.number(n -> {
                                                        n.field("duration");
                                                        if (filters.minDurationSeconds() != null) {
                                                                n.gte(filters.minDurationSeconds());
                                                        }
                                                        if (filters.maxDurationSeconds() != null) {
                                                                n.lt(filters.maxDurationSeconds());
                                                        }
                                                        return n;
                                                })));
                                        }
                                        if (filters.uploadedSince() != null) {
                                                b.filter(range(r -> r.date(d -> d.field("createdAt").gte(filters.uploadedSince()))));
                                        }
                                        return b;
                                }))
                                .withPageable(pageable)
                                .build();

                SearchHits<VideoDocument> hits = elasticsearchOperations.search(query, VideoDocument.class, indexes.videos());

                List<VideoDto> videos = hits.getSearchHits().stream()
                                .map(SearchHit::getContent)
                                .map(doc -> videoRepository.findById(UUID.fromString(doc.getId()))
                                                .filter(v -> VideoAccess.isListed(v.getVideosStatus()))
                                                .map(videoMapper::toDto)
                                                .orElse(null))
                                .filter(Objects::nonNull)
                                .toList();

                log.info("[ELASTIC+MYSQL] Search full by title='{}' {} found {} results in {}ms",
                                text, filters, videos.size(), Duration.between(start, Instant.now()).toMillis());

                return new PageImpl<>(videos, pageable, hits.getTotalHits());
        }

        // ================== HELPER ==================

        private Page<VideoDocument> search(NativeQuery query, Pageable pageable) {
                SearchHits<VideoDocument> hits = elasticsearchOperations.search(query, VideoDocument.class, indexes.videos());
                List<VideoDocument> content = hits.getSearchHits().stream()
                                .map(SearchHit::getContent)
                                .toList();
                return new PageImpl<>(content, pageable, hits.getTotalHits());
        }
}
