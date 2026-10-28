package com.wetube.wetube_service.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.wetube.wetube_service.entity.live.LivePoll;
import com.wetube.wetube_service.entity.live.LivePollVote;
import com.wetube.wetube_service.entity.live.LivePollVoteId;
import com.wetube.wetube_service.entity.live.LiveStream;
import com.wetube.wetube_service.enumeration.LiveProtocol;
import com.wetube.wetube_service.enumeration.LiveSource;
import com.wetube.wetube_service.enumeration.LiveStatus;
import com.wetube.wetube_service.enumeration.RecordingStatus;
import com.wetube.wetube_service.repository.live.LivePollRepository;
import com.wetube.wetube_service.repository.live.LivePollVoteRepository;
import com.wetube.wetube_service.repository.live.LiveStreamRepository;

import jakarta.persistence.EntityManager;

/**
 * Production không tự tạo bảng mới từ entity (log: "Table live_streams doesn't exist"), nên các bảng phát trực tiếp do
 * runner tạo. Hibernate ở đây để ddl-auto=none: nếu DDL của runner lệch entity thì lưu / đọc bên dưới sẽ lỗi.
 */
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.datasource.url=jdbc:h2:mem:liveschema;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class LiveSchemaInitializerTest {

    @Autowired
    PlatformTransactionManager transactionManager;
    @Autowired
    EntityManager em;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    LiveStreamRepository streams;
    @Autowired
    LivePollRepository polls;
    @Autowired
    LivePollVoteRepository votes;

    private void runInitializer() {
        LiveSchemaInitializer initializer = new LiveSchemaInitializer(transactionManager);
        ReflectionTestUtils.setField(initializer, "em", em);
        initializer.run();
    }

    @Test
    void createsTablesThatTheEntitiesCanWriteAndReadBack() {
        runInitializer();
        LocalDateTime now = LocalDateTime.of(2026, 10, 27, 12, 0, 0);
        LiveStream saved = streams.save(LiveStream.builder()
                .channelId(UUID.randomUUID()).ownerId(UUID.randomUUID())
                .title("Buổi phát thử").description("Mô tả dài ".repeat(200))
                .source(LiveSource.ENCODER).protocol(LiveProtocol.WHIP).status(LiveStatus.LIVE)
                .chatEnabled(true).livekitRoom("live-room").ingressId("ing").ingressUrl("https://whip")
                .ingressKey("key").createdAt(now).startedAt(now).endedAt(now).lastPublisherAt(now)
                .currentViewers(3).peakViewers(9).build());

        LiveStream read = streams.findById(saved.getId()).orElseThrow();
        assertThat(read.getTitle()).isEqualTo("Buổi phát thử");
        assertThat(read.getDescription()).hasSize(2000);
        assertThat(read.getSource()).isEqualTo(LiveSource.ENCODER);
        assertThat(read.getProtocol()).isEqualTo(LiveProtocol.WHIP);
        assertThat(read.getStatus()).isEqualTo(LiveStatus.LIVE);
        assertThat(read.isChatEnabled()).isTrue();
        assertThat(read.getIngressKey()).isEqualTo("key");
        assertThat(read.getPeakViewers()).isEqualTo(9);
        assertThat(read.getVersion()).isNotNull();

        LivePoll poll = polls.save(LivePoll.builder().streamId(saved.getId()).question("Chơi gì?")
                .options("[\"a\",\"b\"]").open(true).createdAt(now).build());
        assertThat(polls.findFirstByStreamIdAndOpenTrue(saved.getId())).isPresent();

        votes.saveAndFlush(new LivePollVote(new LivePollVoteId(poll.getId(), UUID.randomUUID()), 1, now));
        assertThat(votes.countByOption(poll.getId())).hasSize(1);
    }

    @Test
    void theVotePrimaryKeyStillRejectsASecondVoteFromTheSameUser() {
        runInitializer();
        UUID pollId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        votes.saveAndFlush(new LivePollVote(new LivePollVoteId(pollId, userId), 0, now));

        String sql = "INSERT INTO live_poll_votes (poll_id, user_id, option_index, created_at) VALUES (?, ?, 1, CURRENT_TIMESTAMP)";
        assertThatThrownBy(() -> jdbc.update(sql, pollId.toString(), userId.toString()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void addsTheThumbnailColumnToAnL1TableThatLacksIt() {
        runInitializer();
        // Bảng như trên production sau L1: có đủ cột trừ thumbnail_url
        jdbc.execute("ALTER TABLE live_streams DROP COLUMN thumbnail_url");

        runInitializer();

        LiveStream saved = streams.save(LiveStream.builder()
                .channelId(UUID.randomUUID()).ownerId(UUID.randomUUID()).title("Có ảnh")
                .source(LiveSource.BROWSER).status(LiveStatus.CREATED).chatEnabled(true)
                .thumbnailUrl("https://res.cloudinary.com/x/image/upload/t.jpg")
                .createdAt(LocalDateTime.now()).build());
        assertThat(streams.findById(saved.getId()).orElseThrow().getThumbnailUrl())
                .isEqualTo("https://res.cloudinary.com/x/image/upload/t.jpg");
    }

    @Test
    void addsTheRecordingColumnsToAnL1TableThatLacksThem() {
        runInitializer();
        // Bảng như trên production sau L1.1: chưa có cột ghi hình
        for (String column : List.of("recording_enabled", "topic_id", "recording_status",
                "recording_started_at", "recording_stopped_at", "recording_seconds", "recording_url",
                "recording_message", "replay_video_id")) {
            jdbc.execute("ALTER TABLE live_streams DROP COLUMN " + column);
        }

        runInitializer();

        UUID videoId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2026, 10, 28, 9, 0, 0);
        LiveStream saved = streams.save(LiveStream.builder()
                .channelId(UUID.randomUUID()).ownerId(UUID.randomUUID()).title("Có ghi hình")
                .source(LiveSource.BROWSER).status(LiveStatus.ENDED).chatEnabled(true).createdAt(now)
                .recordingEnabled(true).topicId("music").recordingStatus(RecordingStatus.DONE)
                .recordingStartedAt(now).recordingStoppedAt(now.plusMinutes(10)).recordingSeconds(600)
                .recordingUrl("https://pub.example/live/a.mp4").recordingMessage("ghi chú").replayVideoId(videoId)
                .build());

        LiveStream read = streams.findById(saved.getId()).orElseThrow();
        assertThat(read.isRecordingEnabled()).isTrue();
        assertThat(read.getTopicId()).isEqualTo("music");
        assertThat(read.getRecordingStatus()).isEqualTo(RecordingStatus.DONE);
        assertThat(read.getRecordingSeconds()).isEqualTo(600);
        assertThat(read.getRecordingUrl()).isEqualTo("https://pub.example/live/a.mp4");
        assertThat(read.getRecordingMessage()).isEqualTo("ghi chú");
        assertThat(read.getReplayVideoId()).isEqualTo(videoId);
        assertThat(streams.sumRecordingSecondsByChannel(saved.getChannelId(), now.minusDays(1), now.plusDays(1)))
                .isEqualTo(600);
        assertThat(streams.sumRecordingSeconds(now.minusDays(1), now.plusDays(1))).isGreaterThanOrEqualTo(600);
        assertThat(streams.sumRecordingSecondsByChannel(saved.getChannelId(), now.plusDays(1), now.plusDays(2)))
                .isZero();
    }

    @Test
    void rowsFromBeforeRecordingGetTheDefaults() {
        runInitializer();
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO live_streams (id, channel_id, owner_id, title, source, status, chat_enabled, "
                + "created_at, current_viewers, peak_viewers) VALUES (?, ?, ?, 'cũ', 'BROWSER', 'ENDED', 1, "
                + "CURRENT_TIMESTAMP, 0, 0)", id.toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString());

        LiveStream read = streams.findById(id).orElseThrow();

        assertThat(read.isRecordingEnabled()).isFalse();
        assertThat(read.getRecordingSeconds()).isZero();
        assertThat(read.getRecordingStatus()).isNull();
        assertThat(read.getReplayVideoId()).isNull();
    }

    @Test
    void runningItTwiceIsHarmless() {
        runInitializer();
        assertThatCode(this::runInitializer).doesNotThrowAnyException();
        assertThat(List.of(streams.count(), polls.count(), votes.count())).allMatch(count -> count >= 0);
    }
}
