package com.wetube.wetube_service.service.video.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.wetube.wetube_service.service.video.impl.FeedRanker.Candidate;

class FeedRankerTest {

    private static final Instant NOW = Instant.parse("2026-10-27T00:00:00Z");

    private static Candidate video(String id, String channel) {
        return new Candidate(id, channel, 0, false, NOW.minus(1, ChronoUnit.DAYS), 100);
    }

    @Test
    void sameSeedGivesTheSameOrderAndADifferentSeedCanChangeIt() {
        List<Candidate> all = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            all.add(video("v" + i, "c" + i));
        }

        List<String> first = FeedRanker.rank(all, "seed-a", Set.of(), NOW);

        assertThat(FeedRanker.rank(all, "seed-a", Set.of(), NOW)).isEqualTo(first);
        assertThat(List.of("seed-b", "seed-c", "seed-d", "seed-e"))
                .anySatisfy(seed -> assertThat(FeedRanker.rank(all, seed, Set.of(), NOW)).isNotEqualTo(first));
    }

    @Test
    void tagMatchesAndSubscribedChannelsOutrankOtherwiseEqualVideos() {
        Candidate plain = video("plain", "c1");
        Candidate tagged = new Candidate("tagged", "c2", 3, false, plain.createdAt(), plain.views());
        Candidate subscribed = new Candidate("subscribed", "c3", 0, true, plain.createdAt(), plain.views());

        List<String> ranked = FeedRanker.rank(List.of(plain, subscribed, tagged), "", Set.of(), NOW);

        assertThat(ranked).containsExactly("tagged", "subscribed", "plain");
    }

    @Test
    void aVideoWatchedAllTheWayDropsBelowFreshOnes() {
        Candidate seen = new Candidate("seen", "c1", 5, true, NOW, 10_000);
        Candidate fresh = video("fresh", "c2");

        assertThat(FeedRanker.rank(List.of(seen, fresh), "", Set.of(), NOW)).containsExactly("seen", "fresh");
        assertThat(FeedRanker.rank(List.of(seen, fresh), "", Set.of("seen"), NOW)).containsExactly("fresh", "seen");
    }

    @Test
    void newerAndMorePopularVideosScoreHigherAllElseEqual() {
        Candidate old = new Candidate("old", "c1", 0, false, NOW.minus(90, ChronoUnit.DAYS), 10);
        Candidate recent = new Candidate("recent", "c2", 0, false, NOW.minus(1, ChronoUnit.DAYS), 10);
        Candidate popular = new Candidate("popular", "c3", 0, false, NOW.minus(1, ChronoUnit.DAYS), 100_000);

        var scores = FeedRanker.scores(List.of(old, recent, popular), "", Set.of(), NOW);

        assertThat(scores.get("recent")).isGreaterThan(scores.get("old"));
        assertThat(scores.get("popular")).isGreaterThan(scores.get("recent"));
    }

    @Test
    void oneChannelIsLimitedToTwoVideosPerWindowOfEightWhileOtherChannelsRemain() {
        List<Candidate> sorted = List.of(
                video("x1", "X"), video("x2", "X"), video("x3", "X"), video("x4", "X"), video("x5", "X"), video("x6", "X"),
                video("y1", "Y"), video("y2", "Y"), video("y3", "Y"));

        List<String> ids = FeedRanker.diversify(sorted).stream().map(Candidate::id).toList();

        // Giới hạn áp dụng cho mọi kênh (cả Y chỉ 2 trong 8). Hết lựa chọn thì lấy video điểm cao nhất còn lại:
        // video bị lùi xuống, không bị bỏ.
        assertThat(ids).containsExactly("x1", "x2", "y1", "y2", "x3", "x4", "x5", "x6", "y3");
    }

    @Test
    void aSingleChannelKeepsEveryVideoInScoreOrder() {
        List<Candidate> sorted = List.of(video("a", "X"), video("b", "X"), video("c", "X"), video("d", "X"));

        assertThat(FeedRanker.diversify(sorted).stream().map(Candidate::id)).containsExactly("a", "b", "c", "d");
    }

    @Test
    void videosWithoutAChannelAreNeverLimited() {
        List<Candidate> sorted = List.of(video("a", null), video("b", null), video("c", null), video("d", null));

        assertThat(FeedRanker.diversify(sorted).stream().map(Candidate::id)).containsExactly("a", "b", "c", "d");
    }

    @Test
    void rankKeepsEveryCandidateExactlyOnce() {
        List<Candidate> all = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            all.add(video("v" + i, "c" + (i % 3)));
        }

        List<String> ranked = FeedRanker.rank(all, "s", Set.of("v1", "v2"), NOW);

        assertThat(ranked).hasSize(30).doesNotHaveDuplicates();
    }
}
