package com.wetube.wetube_service.repository.live;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.wetube.wetube_service.entity.live.LivePollVote;
import com.wetube.wetube_service.entity.live.LivePollVoteId;
import com.wetube.wetube_service.entity.live.LiveStream;
import com.wetube.wetube_service.enumeration.LiveSource;
import com.wetube.wetube_service.enumeration.LiveStatus;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:livetest;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LiveRepositoryTest {

    @Autowired
    LiveStreamRepository streams;
    @Autowired
    LivePollVoteRepository votes;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    jakarta.persistence.EntityManager em;

    private LiveStream stream(UUID channel, LiveStatus status) {
        return streams.save(LiveStream.builder()
                .channelId(channel)
                .ownerId(UUID.randomUUID())
                .title("Buổi phát")
                .source(LiveSource.BROWSER)
                .status(status)
                .chatEnabled(true)
                .livekitRoom("live-" + UUID.randomUUID())
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Test
    void findsOnlyUnfinishedStreamOfChannel() {
        UUID channel = UUID.randomUUID();
        stream(channel, LiveStatus.ENDED);
        LiveStream created = stream(channel, LiveStatus.CREATED);
        stream(UUID.randomUUID(), LiveStatus.LIVE);

        var found = streams.findFirstByChannelIdAndStatusIn(channel, List.of(LiveStatus.CREATED, LiveStatus.LIVE));

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(created.getId());
        assertThat(streams.findFirstByChannelIdAndStatusIn(UUID.randomUUID(),
                List.of(LiveStatus.CREATED, LiveStatus.LIVE))).isEmpty();
    }

    @Test
    void aStaleWriteCannotBringAnEndedStreamBackToLive() {
        LiveStream live = stream(UUID.randomUUID(), LiveStatus.LIVE);
        em.flush();
        em.clear();
        // Job 15 giây đọc bản này rồi đi gọi LiveKit...
        LiveStream staleCopy = streams.findById(live.getId()).orElseThrow();
        em.detach(staleCopy);
        // ...trong lúc đó chủ kênh kết thúc buổi và commit
        LiveStream fresh = streams.findById(live.getId()).orElseThrow();
        fresh.setStatus(LiveStatus.ENDED);
        em.flush();
        em.clear();

        // Ghi muộn dựa trên bản cũ phải bị từ chối chứ không được ghi đè status = LIVE
        staleCopy.setCurrentViewers(7);
        assertThatThrownBy(() -> {
            streams.save(staleCopy);
            em.flush();
        }).isInstanceOf(org.springframework.orm.ObjectOptimisticLockingFailureException.class);
        em.clear();
        assertThat(streams.findById(live.getId()).orElseThrow().getStatus()).isEqualTo(LiveStatus.ENDED);
    }

    @Test
    void pollVoteCountsGroupByOption() {
        UUID poll = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        votes.save(new LivePollVote(new LivePollVoteId(poll, UUID.randomUUID()), 0, now));
        votes.save(new LivePollVote(new LivePollVoteId(poll, UUID.randomUUID()), 0, now));
        votes.save(new LivePollVote(new LivePollVoteId(poll, UUID.randomUUID()), 2, now));
        votes.save(new LivePollVote(new LivePollVoteId(UUID.randomUUID(), UUID.randomUUID()), 0, now));

        Map<Integer, Long> counts = votes.countByOption(poll).stream()
                .collect(Collectors.toMap(row -> ((Number) row[0]).intValue(), row -> ((Number) row[1]).longValue()));

        assertThat(counts).containsExactlyInAnyOrderEntriesOf(Map.of(0, 2L, 2, 1L));
    }

    @Test
    void secondVoteSamePollSameUserViolatesPrimaryKey() {
        String poll = UUID.randomUUID().toString();
        String user = UUID.randomUUID().toString();
        String sql = "INSERT INTO live_poll_votes (poll_id, user_id, option_index, created_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
        jdbc.update(sql, poll, user, 0);

        assertThatThrownBy(() -> jdbc.update(sql, poll, user, 1))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
