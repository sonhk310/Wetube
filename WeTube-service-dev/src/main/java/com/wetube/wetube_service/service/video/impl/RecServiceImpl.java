package com.wetube.wetube_service.service.video.impl;

import com.wetube.wetube_service.dto.video.RecommendVideoDto;
import com.wetube.wetube_service.entity.video.Tag;
import com.wetube.wetube_service.entity.video.UserTag;
import com.wetube.wetube_service.enumeration.PlaylistType;
import com.wetube.wetube_service.mapper.video.VideoMapper;
import com.wetube.wetube_service.repository.PlaylistVideoRepository;
import com.wetube.wetube_service.repository.channel.SubscriptionRepository;
import com.wetube.wetube_service.repository.video.TagRepository;
import com.wetube.wetube_service.repository.video.UserTagRepository;
import com.wetube.wetube_service.search.SearchIndexes;
import com.wetube.wetube_service.search.VideoDocument;
import com.wetube.wetube_service.service.RedisService;
import com.wetube.wetube_service.service.video.RecService;
import com.wetube.wetube_service.service.video.WatchProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import co.elastic.clients.elasticsearch._types.FieldValue;

import static co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.bool;
import static co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.range;
import static co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.terms;

/**
 * Gợi ý trang chủ "giống YouTube": gom ứng viên từ nhiều nguồn (kênh đã đăng ký, thẻ yêu thích, thịnh hành, khám phá),
 * chấm điểm và xếp lại bằng {@link FeedRanker}. Danh sách của mỗi (người, seed) được giữ trong Redis một lúc.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecServiceImpl implements RecService {

    private final UserTagRepository userTagRepo;
    private final TagRepository tagRepo;
    private final ElasticsearchOperations elasticOps;
    /** Chỉ mục video đang dùng (videos_v2); không đặt tên chỉ mục thì Spring Data lấy chỉ mục cũ "videos" ghi trên VideoDocument. */
    private final SearchIndexes indexes;
    private final VideoMapper videoMapper;
    private final RedisService redisService;
    private final PlaylistVideoRepository playlistVideoRepo;
    private final SubscriptionRepository subscriptionRepo;
    private final WatchProgressService watchProgressService;

    /** Danh sách gợi ý của mỗi (người, seed) được giữ trong Redis trong khoảng này (phân trang ổn định, đỡ tải Elasticsearch). */
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private static final int FAVORITE_TAGS = 20;
    private static final int SUBSCRIBED_LIMIT = 60;
    private static final int SUBSCRIBED_DAYS = 60;
    private static final int TAG_LIMIT = 80;
    private static final int TRENDING_LIMIT = 30;
    private static final int TRENDING_DAYS = 30;
    /** Đã xem từ chừng này độ dài trở lên thì coi là xem hết. */
    private static final double WATCHED_RATIO = 0.9;
    private static final int MAX_SEED_LENGTH = 32;

    @Value("${myapp.recommendation.pool-size}")
    private int poolSize;

    @Override
    public Page<RecommendVideoDto> recommendVideos(UUID userId, Pageable pageable, String seed) {
        return pageOf(cachedRecommendedIds(userId, cleanSeed(seed)), pageable);
    }

    @Override
    public Page<RecommendVideoDto> unwatchedVideos(UUID userId, Pageable pageable, String seed) {
        return pageOf(unwatchedIds(userId, cleanSeed(seed)), pageable);
    }

    @Override
    public boolean hasUnwatched(UUID userId, String seed) {
        return !unwatchedIds(userId, cleanSeed(seed)).isEmpty();
    }

    private List<String> unwatchedIds(UUID userId, String seed) {
        Set<String> watched = playlistVideoRepo.findHistoryVideoIds(userId).stream()
                .map(UUID::toString).collect(Collectors.toSet());
        return cachedRecommendedIds(userId, seed).stream().filter(id -> !watched.contains(id)).toList();
    }

    private Page<RecommendVideoDto> pageOf(List<String> sortedVideoIds, Pageable pageable) {
        long startOffset = pageable.getOffset();
        if (startOffset >= sortedVideoIds.size()) {
            return Page.empty(pageable);
        }
        long toIndex = Math.min(startOffset + pageable.getPageSize(), sortedVideoIds.size());
        List<String> pageVideoIds = sortedVideoIds.subList((int) startOffset, (int) toIndex);
        return new PageImpl<>(findVideosByIdsAndPreserveOrder(pageVideoIds), pageable, sortedVideoIds.size());
    }

    @Override
    public void evictRecommendations(UUID userId) {
        // Khoá danh sách có kèm "phiên bản" của người dùng: đổi phiên bản là bỏ hết danh sách cũ (mọi seed)
        redisService.set(versionKey(userId), String.valueOf(System.currentTimeMillis()), Duration.ofDays(1));
    }

    private static String versionKey(UUID userId) {
        return "rec:ver:" + userId;
    }

    private String cacheKey(UUID userId, String seed) {
        String version = redisService.get(versionKey(userId));
        return "rec:" + userId + ":" + (version == null ? "0" : version) + ":" + seed;
    }

    /** Seed do client gửi chỉ được là chữ / số / - / _ tối đa 32 ký tự (đi vào khoá Redis); sai thì coi như không có. */
    static String cleanSeed(String seed) {
        if (seed == null) {
            return "";
        }
        String s = seed.trim();
        return s.length() <= MAX_SEED_LENGTH && s.matches("[A-Za-z0-9_-]*") ? s : "";
    }

    private List<String> cachedRecommendedIds(UUID userId, String seed) {
        String key = cacheKey(userId, seed);
        String cached = redisService.get(key);
        if (cached != null) {
            return cached.isEmpty() ? List.of() : List.of(cached.split(","));
        }
        // Như YouTube: chưa xem video nào (lịch sử trống) thì chưa gợi ý, trang chủ mời tìm kiếm.
        // Không lưu cache để xem xong video đầu tiên là có gợi ý ngay.
        if (!playlistVideoRepo.existsByPlaylist_User_IdAndPlaylist_PlaylistType(userId, PlaylistType.HISTORY)) {
            return List.of();
        }
        List<String> ids = rankedIdsFor(userId, seed);
        redisService.set(key, String.join(",", ids), CACHE_TTL);
        return ids;
    }

    private List<String> rankedIdsFor(UUID userId, String seed) {
        log.info("CACHE MISS! Generating new recommendation list for user {} (seed '{}', pool size {}).", userId, seed, poolSize);

        List<String> userTags = getUserFavoriteTags(userId);
        Set<String> subscribed = subscriptionRepo.findSubscribedChannelIds(userId).stream()
                .map(UUID::toString).collect(Collectors.toSet());

        Map<String, VideoDocument> pool = new LinkedHashMap<>();
        if (!subscribed.isEmpty()) {
            searchSubscribed(subscribed).forEach(d -> pool.putIfAbsent(d.getId(), d));
        }
        if (!userTags.isEmpty()) {
            searchVideosInElasticsearch(userTags, Pageable.ofSize(TAG_LIMIT)).getSearchHits()
                    .forEach(h -> pool.putIfAbsent(h.getContent().getId(), h.getContent()));
        }
        searchTrending().forEach(d -> pool.putIfAbsent(d.getId(), d));
        // Khám phá: lấp phần còn lại bằng video mới nhất, thứ tự ngẫu nhiên theo seed
        List<VideoDocument> rest = new ArrayList<>(latestDocs().stream().filter(d -> !pool.containsKey(d.getId())).toList());
        Collections.shuffle(rest, new Random(seed.hashCode()));
        for (VideoDocument d : rest) {
            if (pool.size() >= poolSize) {
                break;
            }
            pool.put(d.getId(), d);
        }
        pool.values().removeIf(d -> userId.toString().equals(d.getUserId()));
        if (pool.isEmpty()) {
            return List.of();
        }

        List<VideoDocument> docs = new ArrayList<>(pool.values());
        return FeedRanker.rank(toCandidates(docs, userTags, subscribed), seed, watchedFully(userId, docs), Instant.now());
    }

    private static List<FeedRanker.Candidate> toCandidates(List<VideoDocument> docs, List<String> userTags, Set<String> subscribed) {
        Set<String> favorite = new HashSet<>(userTags);
        return docs.stream().map(d -> new FeedRanker.Candidate(
                d.getId(),
                d.getChannelId(),
                d.getTags() == null ? 0 : (int) d.getTags().stream().filter(favorite::contains).count(),
                d.getChannelId() != null && subscribed.contains(d.getChannelId()),
                d.getCreatedAt(),
                d.getTotalView() == null ? 0 : d.getTotalView())).toList();
    }

    /** Video người dùng đã xem gần hết (từ 90% độ dài): bị hạ điểm. Lỗi thì coi như chưa xem video nào hết. */
    private Set<String> watchedFully(UUID userId, List<VideoDocument> docs) {
        try {
            Map<UUID, Float> positions = watchProgressService.getMany(userId,
                    docs.stream().map(d -> UUID.fromString(d.getId())).toList());
            Set<String> result = new HashSet<>();
            for (VideoDocument d : docs) {
                Float position = positions.get(UUID.fromString(d.getId()));
                if (position != null && d.getDuration() != null && d.getDuration() > 0
                        && position >= WATCHED_RATIO * d.getDuration()) {
                    result.add(d.getId());
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Không đọc được tiến độ xem của người dùng {}: {}", userId, e.getMessage());
            return Set.of();
        }
    }

    /** Video mới của các kênh đã đăng ký trong SUBSCRIBED_DAYS ngày gần đây. */
    private List<VideoDocument> searchSubscribed(Set<String> channelIds) {
        NativeQuery query = new NativeQueryBuilder()
                .withQuery(bool(b -> b
                        .filter(terms(t -> t.field("channelId")
                                .terms(f -> f.value(channelIds.stream().map(FieldValue::of).toList()))))
                        .filter(range(r -> r.date(d -> d.field("createdAt").gte("now-" + SUBSCRIBED_DAYS + "d"))))))
                .withPageable(PageRequest.of(0, SUBSCRIBED_LIMIT, Sort.by(Sort.Order.desc("createdAt"))))
                .build();
        return elasticOps.search(query, VideoDocument.class, indexes.videos()).getSearchHits().stream()
                .map(SearchHit::getContent).toList();
    }

    /** Nhiều lượt xem nhất trong TRENDING_DAYS ngày gần đây; kho còn ít video mới thì lấy theo mọi thời gian. */
    private List<VideoDocument> searchTrending() {
        List<VideoDocument> recent = trending(true);
        return recent.isEmpty() ? trending(false) : recent;
    }

    private List<VideoDocument> trending(boolean recentOnly) {
        NativeQueryBuilder builder = new NativeQueryBuilder()
                .withPageable(PageRequest.of(0, TRENDING_LIMIT, Sort.by(Sort.Order.desc("totalView"))));
        if (recentOnly) {
            builder.withQuery(range(r -> r.date(d -> d.field("createdAt").gte("now-" + TRENDING_DAYS + "d"))));
        }
        return elasticOps.search(builder.build(), VideoDocument.class, indexes.videos()).getSearchHits().stream()
                .map(SearchHit::getContent).toList();
    }

    /** Video công khai mới nhất (tối đa poolSize). */
    private List<VideoDocument> latestDocs() {
        CriteriaQuery query = new CriteriaQuery(new Criteria());
        query.setPageable(Pageable.ofSize(this.poolSize));
        query.addSort(Sort.by(Sort.Order.desc("createdAt")));
        return elasticOps.search(query, VideoDocument.class, indexes.videos()).getSearchHits().stream()
                .map(SearchHit::getContent).toList();
    }

    @Override
    public List<RecommendVideoDto> findTopRankedVideos(UUID userId, int poolSize, int topN) {
        List<String> userTags = getUserFavoriteTags(userId);
        if (userTags.isEmpty()) {
            return Collections.emptyList();
        }

        SearchHits<VideoDocument> hits = searchVideosInElasticsearch(userTags, Pageable.ofSize(poolSize));
        if (hits.isEmpty()) {
            return Collections.emptyList();
        }
        List<VideoDocument> docs = hits.getSearchHits().stream().map(SearchHit::getContent).toList();
        Map<String, VideoDocument> byId = docs.stream().collect(Collectors.toMap(VideoDocument::getId, d -> d, (a, b) -> a));
        List<String> ranked = FeedRanker.rank(toCandidates(docs, userTags, Set.of()), "", Set.of(), Instant.now());

        return videoMapper.toRecommendDtoListFromDoc(ranked.stream().limit(topN).map(byId::get).toList());
    }

    private List<String> getUserFavoriteTags(UUID userId) {
        List<UserTag> affinities = userTagRepo.findAllByUserIdOrderByPointDesc(userId);
        if (affinities.isEmpty()) {
            log.info("User {} has no tag affinities, cannot recommend by tags.", userId);
            return Collections.emptyList();
        }

        return affinities.stream()
                .limit(FAVORITE_TAGS)
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

        return elasticOps.search(query, VideoDocument.class, indexes.videos());
    }

    private List<RecommendVideoDto> findVideosByIdsAndPreserveOrder(List<String> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }

        Criteria criteria = new Criteria("id").in(ids);
        CriteriaQuery query = new CriteriaQuery(criteria);
        // Không đặt thì Spring Data chỉ lấy 10 kết quả: trang 18 video sẽ thiếu 8
        query.setPageable(Pageable.ofSize(ids.size()));
        SearchHits<VideoDocument> hits = elasticOps.search(query, VideoDocument.class, indexes.videos());

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
