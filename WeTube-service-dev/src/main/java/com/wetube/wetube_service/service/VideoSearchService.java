package com.wetube.wetube_service.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Like;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
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
import com.wetube.wetube_service.entity.video.Video;
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
                        // _score trước: kết quả khớp chặt (điểm 1000) luôn đứng trên kết quả khớp lỏng (điểm 1)
                        case DATE -> PageRequest.of(page, size,
                                        Sort.by(Sort.Order.desc("_score"), Sort.Order.desc("createdAt")));
                        case VIEWS -> PageRequest.of(page, size,
                                        Sort.by(Sort.Order.desc("_score"), Sort.Order.desc("totalView")));
                        case RELEVANCE -> PageRequest.of(page, size);
                };

                SearchHits<VideoDocument> hits = searchHits(text, filters, pageable);
                List<VideoDto> videos = toListedDtos(hits.getSearchHits().stream().map(SearchHit::getContent).toList());

                log.info("[ELASTIC+MYSQL] Search full by title='{}' {} found {} results in {}ms",
                                text, filters, videos.size(), Duration.between(start, Instant.now()).toMillis());

                return new PageImpl<>(videos, pageable, hits.getTotalHits());
        }

        /**
         * "Video tương tự": lấy 3 kết quả đầu của chính câu tìm này, tìm video giống chúng (more_like_this) và
         * bỏ các video đã nằm trong {@code mainSize} kết quả đầu (trang kết quả chính).
         */
        public List<VideoDto> similarVideos(String text, SearchFilters filters, int mainSize, int size) {
                Pageable main = PageRequest.of(0, mainSize);
                List<String> mainIds = searchHits(text, filters, main).getSearchHits().stream()
                                .map(h -> h.getContent().getId()).toList();
                if (mainIds.isEmpty()) {
                        return List.of();
                }
                String indexName = indexes.videos().getIndexName();
                List<Like> like = mainIds.stream().limit(SIMILAR_SEEDS)
                                .map(id -> Like.of(l -> l.document(d -> d.index(indexName).id(id)))).toList();

                NativeQuery query = new NativeQueryBuilder()
                                .withQuery(bool(b -> {
                                        // min_term_freq / min_doc_freq = 1 vì kho video còn nhỏ
                                        b.must(moreLikeThis(m -> m
                                                        .fields("title", "tags.text", "description", "channelName", "searchKeywords")
                                                        .like(like).minTermFreq(1).minDocFreq(1).maxQueryTerms(25))
                                                        );
                                        b.mustNot(ids(i -> i.values(mainIds)));
                                        addFilters(b, filters);
                                        return b;
                                }))
                                .withPageable(PageRequest.of(0, size))
                                .build();
                SearchHits<VideoDocument> hits = elasticsearchOperations.search(query, VideoDocument.class, indexes.videos());
                return toListedDtos(hits.getSearchHits().stream().map(SearchHit::getContent).toList());
        }

        // ================== TRUY VẤN ==================

        /** Số kết quả đầu dùng làm mẫu cho "Video tương tự". */
        private static final int SIMILAR_SEEDS = 3;

        private SearchHits<VideoDocument> searchHits(String text, SearchFilters filters, Pageable pageable) {
                NativeQuery query = new NativeQueryBuilder()
                                .withQuery(bool(b -> {
                                        tiered(b, text, filters.sort() == SearchFilters.Sort.RELEVANCE);
                                        addFilters(b, filters);
                                        return b;
                                }))
                                .withPageable(pageable)
                                .build();
                return elasticsearchOperations.search(query, VideoDocument.class, indexes.videos());
        }

        /**
         * Hai tầng: khớp chặt (xếp trên) và khớp lỏng (xếp dưới). Điểm tầng chặt luôn >= 1000, tầng lỏng thấp hơn
         * nhiều. Xếp theo độ liên quan thì trong từng tầng vẫn xếp theo điểm thật; xếp theo ngày / lượt xem thì
         * điểm mỗi tầng là hằng số (1000 / 1) để sắp xếp thứ hai (ngày, lượt xem) có tác dụng trong từng tầng.
         */
        static void tiered(BoolQuery.Builder b, String text, boolean byRelevance) {
                Query strict = strictQuery(text);
                Query loose = looseQuery(text);
                b.should(constantScore(c -> c.filter(strict).boost(1000f)));
                if (byRelevance) {
                        b.should(strict).should(loose);
                } else {
                        Query looseOnly = bool(x -> x.must(loose).mustNot(strict));
                        b.should(constantScore(c -> c.filter(looseOnly).boost(1f)));
                }
                // Có filter thì should thành tuỳ chọn: bắt buộc khớp ít nhất 1
                b.minimumShouldMatch("1");
        }

        /**
         * Khớp chặt: cả cụm khớp tiêu đề; hoặc mọi từ khớp ở tiêu đề / tên kênh / thẻ / từ khóa tìm kiếm (sai chính tả
         * chỉ cho từ dài: dưới 5 chữ phải đúng, 5-7 chữ sai 1, từ 8 chữ sai 2); hoặc từ 80% mẩu 3 chữ khớp (viết dính).
         */
        static Query strictQuery(String text) {
                return bool(b -> b
                                .should(matchPhrase(m -> m.field("title").query(text).boost(6f)))
                                .should(multiMatch(m -> m
                                                .fields("title^3", "channelName^2", "tags.text^2", "searchKeywords^2")
                                                .query(text).operator(Operator.And).fuzziness("AUTO:5,8")))
                                .should(match(m -> m.field("title.compact").query(text).minimumShouldMatch("80%")))
                                .should(match(m -> m.field("channelName.compact").query(text).minimumShouldMatch("80%")))
                                .should(match(m -> m.field("searchKeywords.compact").query(text).minimumShouldMatch("80%")))
                                .should(wildcard(w -> w.field("title").value(text.toLowerCase() + "*")))
                                .minimumShouldMatch("1"));
        }

        /**
         * Khớp lỏng: sai chính tả mờ với mọi từ, cả mô tả, nhưng câu 1-2 từ phải khớp đủ từ, câu dài khớp 75% số từ;
         * hoặc 60% mẩu 3 chữ. Các trường mới chỉ có ở videos_v2.
         */
        static Query looseQuery(String text) {
                return bool(b -> b
                                .should(multiMatch(m -> m
                                                .fields("title^3", "channelName^2", "tags.text^2", "searchKeywords^2", "description")
                                                .query(text).fuzziness("AUTO").minimumShouldMatch("2<75%")))
                                .should(match(m -> m.field("title.compact").query(text).minimumShouldMatch("60%")))
                                .should(match(m -> m.field("channelName.compact").query(text).minimumShouldMatch("60%")))
                                .should(match(m -> m.field("searchKeywords.compact").query(text).minimumShouldMatch("60%")))
                                .minimumShouldMatch("1"));
        }

        private static void addFilters(BoolQuery.Builder b, SearchFilters filters) {
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
        }

        /** Đọc các video của trang bằng một câu truy vấn MySQL, giữ thứ tự của Elasticsearch, bỏ video không còn công khai. */
        private List<VideoDto> toListedDtos(List<VideoDocument> docs) {
                List<UUID> ids = docs.stream().map(d -> UUID.fromString(d.getId())).toList();
                Map<UUID, Video> byId = videoRepository.findAllById(ids).stream()
                                .collect(Collectors.toMap(Video::getId, v -> v));
                return ids.stream()
                                .map(byId::get)
                                .filter(Objects::nonNull)
                                .filter(v -> VideoAccess.isListed(v.getVideosStatus()))
                                .map(videoMapper::toDto)
                                .toList();
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
