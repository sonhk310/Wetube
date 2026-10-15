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
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.stereotype.Service;

import com.wetube.wetube_service.dto.video.VideoDto;
import com.wetube.wetube_service.mapper.video.VideoMapper;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.search.VideoDocument;

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

        private static final IndexCoordinates INDEX = IndexCoordinates.of("videos");

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

                SearchHits<VideoDocument> hits = elasticsearchOperations.search(query, VideoDocument.class, INDEX);
                return hits.hasSearchHits() ? hits.getSearchHits().get(0).getContent() : null;
        }

        public List<String> suggestTitles(String prefix, int size) {
                NativeQuery query = new NativeQueryBuilder()
                                .withQuery(multiMatch(m -> m
                                                .fields("title", "description", "tags", "name")
                                                .query(prefix)
                                                .fuzziness("AUTO")))
                                .withSourceFilter(new FetchSourceFilter(true, new String[]{"title"}, new String[]{}))
                                .withMaxResults(size)
                                .build();

                SearchHits<VideoDocument> hits = elasticsearchOperations.search(query, VideoDocument.class, INDEX);

                return hits.getSearchHits().stream()
                                .map(h -> h.getContent().getTitle())
                                .filter(Objects::nonNull)
                                .distinct()
                                .toList();
        }

        // ================== SEARCH TRẢ VỀ VIDEO DTO (LOAD FULL TỪ MYSQL)
        // ==================

        public Page<VideoDto> searchByTitleFull(String text, int page, int size) {
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

                SearchHits<VideoDocument> hits = elasticsearchOperations.search(query, VideoDocument.class, INDEX);

                List<VideoDto> videos = hits.getSearchHits().stream()
                                .map(SearchHit::getContent)
                                .map(doc -> videoRepository.findById(UUID.fromString(doc.getId()))
                                                .map(videoMapper::toDto)
                                                .orElse(null))
                                .filter(Objects::nonNull)
                                .toList();

                log.info("[ELASTIC+MYSQL] Search full by title='{}' found {} results in {}ms",
                                text, videos.size(), Duration.between(start, Instant.now()).toMillis());

                return new PageImpl<>(videos, pageable, hits.getTotalHits());
        }

        // ================== HELPER ==================

        private Page<VideoDocument> search(NativeQuery query, Pageable pageable) {
                SearchHits<VideoDocument> hits = elasticsearchOperations.search(query, VideoDocument.class, INDEX);
                List<VideoDocument> content = hits.getSearchHits().stream()
                                .map(SearchHit::getContent)
                                .toList();
                return new PageImpl<>(content, pageable, hits.getTotalHits());
        }
}
