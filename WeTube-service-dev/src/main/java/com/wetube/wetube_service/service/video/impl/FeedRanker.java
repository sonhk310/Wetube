package com.wetube.wetube_service.service.video.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * Chấm điểm và xếp thứ tự gợi ý trang chủ (hàm thuần, không phụ thuộc Elasticsearch / MySQL).
 * điểm = 0,40·khớp thẻ + 0,20·kênh đã đăng ký + 0,15·độ mới + 0,10·độ phổ biến + 0,15·ngẫu nhiên (theo seed);
 * video đã xem hết nhân 0,2; sau đó một kênh chỉ được tối đa 2 video trong mỗi 8 vị trí liên tiếp.
 */
final class FeedRanker {

    static final double W_TAG = 0.40;
    static final double W_SUBSCRIBED = 0.20;
    static final double W_FRESH = 0.15;
    static final double W_POPULAR = 0.10;
    static final double W_RANDOM = 0.15;
    static final double WATCHED_FACTOR = 0.2;
    /** Một kênh tối đa MAX_PER_WINDOW video trong mỗi WINDOW vị trí liên tiếp. */
    static final int WINDOW = 8;
    static final int MAX_PER_WINDOW = 2;

    record Candidate(String id, String channelId, int tagMatches, boolean subscribed, Instant createdAt, int views) {
    }

    private FeedRanker() {
    }

    /** Số ngẫu nhiên trong [0, 1) cố định theo (seed, video): cùng seed thì cùng thứ tự, đổi seed thì đổi. */
    static double jitter(String seed, String videoId) {
        return new SplittableRandom(seed.hashCode() * 31L + videoId.hashCode()).nextDouble();
    }

    static Map<String, Double> scores(List<Candidate> candidates, String seed, Set<String> watchedFully, Instant now) {
        double maxTags = Math.max(1, candidates.stream().mapToInt(Candidate::tagMatches).max().orElse(1));
        double maxViews = Math.max(1, candidates.stream().mapToInt(c -> Math.max(0, c.views())).max().orElse(1));
        Map<String, Double> result = new HashMap<>();
        for (Candidate c : candidates) {
            double freshness = 0.5;
            if (c.createdAt() != null) {
                long ageDays = Math.max(0, Duration.between(c.createdAt(), now).toDays());
                freshness = Math.exp(-0.08 * ageDays);
            }
            double popularity = Math.log1p(Math.max(0, c.views())) / Math.log1p(maxViews);
            double score = W_TAG * (c.tagMatches() / maxTags)
                    + W_SUBSCRIBED * (c.subscribed() ? 1 : 0)
                    + W_FRESH * freshness
                    + W_POPULAR * popularity
                    + W_RANDOM * jitter(seed, c.id());
            if (watchedFully.contains(c.id())) {
                score *= WATCHED_FACTOR;
            }
            result.put(c.id(), score);
        }
        return result;
    }

    /** Id video theo thứ tự hiển thị: điểm cao trước, rồi giãn để không kênh nào chiếm cả màn hình. */
    static List<String> rank(List<Candidate> candidates, String seed, Set<String> watchedFully, Instant now) {
        Map<String, Double> score = scores(candidates, seed, watchedFully, now);
        List<Candidate> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator.comparingDouble((Candidate c) -> score.get(c.id())).reversed()
                .thenComparing(Candidate::id));
        return diversify(sorted).stream().map(Candidate::id).toList();
    }

    /**
     * Đi từ trên xuống, mỗi vị trí chọn video điểm cao nhất mà kênh của nó chưa đủ MAX_PER_WINDOW video trong
     * WINDOW - 1 vị trí liền trước. Hết lựa chọn (vd. chỉ còn một kênh) thì lấy video điểm cao nhất: video bị lùi
     * xuống, không bị bỏ.
     */
    static List<Candidate> diversify(List<Candidate> sortedByScore) {
        List<Candidate> remaining = new ArrayList<>(sortedByScore);
        List<Candidate> out = new ArrayList<>(sortedByScore.size());
        while (!remaining.isEmpty()) {
            Map<String, Integer> recent = new HashMap<>();
            for (int i = Math.max(0, out.size() - (WINDOW - 1)); i < out.size(); i++) {
                String channel = out.get(i).channelId();
                if (channel != null) {
                    recent.merge(channel, 1, Integer::sum);
                }
            }
            Candidate pick = remaining.get(0);
            for (Candidate c : remaining) {
                if (c.channelId() == null || recent.getOrDefault(c.channelId(), 0) < MAX_PER_WINDOW) {
                    pick = c;
                    break;
                }
            }
            remaining.remove(pick);
            out.add(pick);
        }
        return out;
    }
}
