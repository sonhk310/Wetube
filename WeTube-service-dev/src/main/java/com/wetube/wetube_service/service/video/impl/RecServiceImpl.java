package com.wetube.wetube_service.service.video.impl;

import com.wetube.wetube_service.dto.video.RecommendVideoDto;
import com.wetube.wetube_service.entity.video.Tag;
import com.wetube.wetube_service.entity.video.UserTag;
import com.wetube.wetube_service.mapper.video.VideoMapper;
import com.wetube.wetube_service.repository.video.TagRepository;
import com.wetube.wetube_service.repository.video.UserTagRepository;
import com.wetube.wetube_service.search.VideoDocument;
import com.wetube.wetube_service.service.video.RecService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecServiceImpl implements RecService {

    private final UserTagRepository userTagRepo;
    private final TagRepository tagRepo;
    private final ElasticsearchOperations elasticOps;
    private final VideoMapper videoMapper;

    @Value("${myapp.recommendation.pool-size}")
    private int poolSize;

    @Override
    public Page<RecommendVideoDto> recommendVideos(UUID userId, Pageable pageable) {
        List<String> sortedVideoIds = getSortedRecommendedIdsForUser(userId);

        long startOffset = pageable.getOffset();
        if (startOffset >= sortedVideoIds.size()) {
            return Page.empty(pageable);
        }

        long endOffset = startOffset + pageable.getPageSize() - 1;
        long toIndex = Math.min(endOffset + 1, sortedVideoIds.size());
        List<String> pageVideoIds = sortedVideoIds.subList((int) startOffset, (int) toIndex);

        List<RecommendVideoDto> dtoList = findVideosByIdsAndPreserveOrder(pageVideoIds);

        return new PageImpl<>(dtoList, pageable, sortedVideoIds.size());
    }

    @Cacheable(value = "recommendations", key = "#userId")
    public List<String> getSortedRecommendedIdsForUser(UUID userId) {
        log.info("CACHE MISS! Generating new recommendation list for user {} with pool size {}.", userId, poolSize);

        List<String> userTags = getUserFavoriteTags(userId);
        if (userTags.isEmpty()) return Collections.emptyList();

        SearchHits<VideoDocument> hits = searchVideosInElasticsearch(userTags, Pageable.ofSize(this.poolSize));
        if (hits.isEmpty()) return Collections.emptyList();

        List<VideoDocument> poolOfDocs = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();

        List<VideoDocument> sortedDocs = scoreAndSortVideos(poolOfDocs, userTags);

        return sortedDocs.stream().map(VideoDocument::getId).toList();
    }

    @Override
    public List<RecommendVideoDto> findTopRankedVideos(UUID userId, int poolSize, int topN){
        List<String> userTags = getUserFavoriteTags(userId);
        if (userTags.isEmpty()) {
            return Collections.emptyList();
        }

        SearchHits<VideoDocument> hits = searchVideosInElasticsearch(userTags, Pageable.ofSize(poolSize));
        if (hits.isEmpty()) {
            return Collections.emptyList();
        }
        List<VideoDocument> wideViewDocs = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();

        List<VideoDocument> sortedDocs = scoreAndSortVideos(wideViewDocs, userTags);

        List<VideoDocument> topDocs = sortedDocs.stream().limit(topN).toList();

        return videoMapper.toRecommendDtoListFromDoc(topDocs);
    }

    private List<String> getUserFavoriteTags(UUID userId) {
        List<UserTag> affinities = userTagRepo.findAllByUserIdOrderByPointDesc(userId);
        if (affinities.isEmpty()) {
            log.info("User {} has no tag affinities, cannot recommend.", userId);
            return Collections.emptyList();
        }

        return affinities.stream()
                .map(ut -> tagRepo.findById(ut.getTagId())
                        .map(Tag::getName)
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    private SearchHits<VideoDocument> searchVideosInElasticsearch(List<String> userTags, Pageable pageable) {
        log.debug("Searching videos with tags: {}", userTags);

        Criteria criteria = new Criteria("tags").in(userTags)
                .or(new Criteria("title").matches(String.join(" ", userTags)))
                .or(new Criteria("description").matches(String.join(" ", userTags)));

        CriteriaQuery query = new CriteriaQuery(criteria);
        query.setPageable(pageable);
        query.addSort(Sort.by(Sort.Order.desc("_score")));
        query.addSort(Sort.by(Sort.Order.desc("createdAt")));

        return elasticOps.search(query, VideoDocument.class);
    }

    private List<VideoDocument> scoreAndSortVideos(List<VideoDocument> docs, List<String> userTags) {
        Map<String, Double> videoTagSum = new HashMap<>();
        double maxTagSum = 1.0;
        for (VideoDocument v : docs) {
            double sum = 0;
            if (v.getTags() != null) {
                sum = v.getTags().stream().filter(userTags::contains).count();
            }
            videoTagSum.put(v.getId(), sum);
            if (sum > maxTagSum) {
                maxTagSum = sum;
            }
        }

        record ScoredVideo(VideoDocument video, double score) {}

        double finalMaxTagSum = maxTagSum;
        List<ScoredVideo> scoredList = docs.stream().map(v -> {
            double tagAffinity = videoTagSum.getOrDefault(v.getId(), 0.0) / finalMaxTagSum;
            double freshness = 0.5;
            if (v.getCreatedAt() != null) {
                long ageDays = Duration.between(v.getCreatedAt(), Instant.now()).toDays();
                freshness = Math.exp(-0.08 * Math.max(0, ageDays));
            }
            double finalScore = 0.70 * tagAffinity + 0.30 * freshness;
            return new ScoredVideo(v, finalScore);
        }).toList();

        return scoredList.stream()
                .sorted(Comparator.comparingDouble(ScoredVideo::score).reversed())
                .map(ScoredVideo::video)
                .toList();
    }

    private List<RecommendVideoDto> findVideosByIdsAndPreserveOrder(List<String> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }

        Criteria criteria = new Criteria("id").in(ids);
        CriteriaQuery query = new CriteriaQuery(criteria);
        SearchHits<VideoDocument> hits = elasticOps.search(query, VideoDocument.class);

        Map<String, VideoDocument> docMap = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toMap(VideoDocument::getId, doc -> doc));

        List<VideoDocument> orderedDocs = ids.stream()
                .map(docMap::get)
                .filter(Objects::nonNull)
                .toList();

        return videoMapper.toRecommendDtoListFromDoc(orderedDocs);
    }
}