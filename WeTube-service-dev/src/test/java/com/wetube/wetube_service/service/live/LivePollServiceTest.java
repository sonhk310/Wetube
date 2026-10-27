package com.wetube.wetube_service.service.live;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wetube.wetube_service.dto.live.LiveEvent;
import com.wetube.wetube_service.dto.live.OpenPollRequest;
import com.wetube.wetube_service.dto.live.PollView;
import com.wetube.wetube_service.entity.live.LivePoll;
import com.wetube.wetube_service.entity.live.LivePollVote;
import com.wetube.wetube_service.entity.live.LivePollVoteId;
import com.wetube.wetube_service.entity.live.LiveStream;
import com.wetube.wetube_service.enumeration.LiveSource;
import com.wetube.wetube_service.enumeration.LiveStatus;
import com.wetube.wetube_service.repository.live.LivePollRepository;
import com.wetube.wetube_service.repository.live.LivePollVoteRepository;
import com.wetube.wetube_service.repository.live.LiveStreamRepository;
import com.wetube.wetube_service.security.RateLimiter;
import com.wetube.wetube_service.security.RateLimiter.Limit;

class LivePollServiceTest {

    private final LiveStreamRepository streams = mock(LiveStreamRepository.class);
    private final LivePollRepository polls = mock(LivePollRepository.class);
    private final LivePollVoteRepository votes = mock(LivePollVoteRepository.class);
    private final RateLimiter rateLimiter = mock(RateLimiter.class);
    private final LiveEventPublisher publisher = mock(LiveEventPublisher.class);

    private final UUID owner = UUID.randomUUID();
    private final UUID fan = UUID.randomUUID();
    private final LocalDateTime now = LocalDateTime.of(2026, 10, 27, 12, 0, 0);
    private final List<LivePoll> stored = new ArrayList<>();
    private LiveStream stream;
    private LivePollService service;

    @BeforeEach
    void setUp() {
        stream = LiveStream.builder().id(UUID.randomUUID()).channelId(UUID.randomUUID()).ownerId(owner)
                .title("Buổi phát").source(LiveSource.BROWSER).status(LiveStatus.LIVE).chatEnabled(true)
                .createdAt(now.minusHours(1)).build();
        when(streams.findById(stream.getId())).thenReturn(Optional.of(stream));
        when(polls.save(any(LivePoll.class))).thenAnswer(inv -> {
            LivePoll p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(UUID.randomUUID());
                stored.add(p);
            }
            return p;
        });
        when(polls.findById(any())).thenAnswer(inv -> stored.stream().filter(p -> p.getId().equals(inv.getArgument(0))).findFirst());
        when(polls.findFirstByStreamIdAndOpenTrue(stream.getId()))
                .thenAnswer(inv -> stored.stream().filter(LivePoll::isOpen).findFirst());
        when(polls.findByStreamIdOrderByCreatedAtAsc(stream.getId())).thenAnswer(inv -> List.copyOf(stored));
        when(votes.countByOption(any())).thenReturn(List.of());
        when(votes.findById(any())).thenReturn(Optional.empty());
        service = new LivePollService(streams, polls, votes, rateLimiter, publisher, new ObjectMapper());
    }

    private static OpenPollRequest poll(String question, String... options) {
        return new OpenPollRequest(question, List.of(options));
    }

    private PollView open() {
        return service.open(owner, stream.getId(), poll("Chơi game nào?", "Liên Minh", "Valorant", "Minecraft"));
    }

    private static HttpStatus statusOf(Throwable t) {
        return HttpStatus.valueOf(((ResponseStatusException) t).getStatusCode().value());
    }

    private List<LiveEvent> pollEvents() {
        ArgumentCaptor<LiveEvent> captor = ArgumentCaptor.forClass(LiveEvent.class);
        verify(publisher, atLeast(0)).toStream(eq(stream.getId()), captor.capture());
        return captor.getAllValues().stream().filter(e -> "POLL".equals(e.getType())).toList();
    }

    // ------------------------------------------------------------------ mở / đóng

    @Test
    void openCreatesAnOpenPollAndBroadcastsIt() {
        PollView view = open();

        assertThat(view.open()).isTrue();
        assertThat(view.question()).isEqualTo("Chơi game nào?");
        assertThat(view.options()).containsExactly("Liên Minh", "Valorant", "Minecraft");
        assertThat(view.counts()).containsExactly(0, 0, 0);
        assertThat(view.total()).isZero();
        assertThat(pollEvents()).hasSize(1);
        assertThat(pollEvents().get(0).getPoll().open()).isTrue();
    }

    @Test
    void onlyTheOwnerCanOpenAndClose() {
        PollView view = open();
        UUID stranger = UUID.randomUUID();

        assertThatThrownBy(() -> service.open(stranger, stream.getId(), poll("?", "a", "b")))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> service.close(stranger, stream.getId(), UUID.fromString(view.id())))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void aSecondPollWhileOneIsOpenIs409() {
        open();

        assertThatThrownBy(() -> service.open(owner, stream.getId(), poll("Lại?", "a", "b")))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void aNewPollCanOpenAfterTheOldOneIsClosed() {
        PollView first = open();
        service.close(owner, stream.getId(), UUID.fromString(first.id()));

        PollView second = service.open(owner, stream.getId(), poll("Tiếp?", "có", "không"));

        assertThat(second.open()).isTrue();
        assertThat(second.id()).isNotEqualTo(first.id());
    }

    @Test
    void needsBetweenTwoAndFourOptions() {
        assertThatThrownBy(() -> service.open(owner, stream.getId(), poll("?", "chỉ một")))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.open(owner, stream.getId(), poll("?", "1", "2", "3", "4", "5")))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.BAD_REQUEST));

        assertThat(service.open(owner, stream.getId(), poll("?", "1", "2")).options()).hasSize(2);
    }

    @Test
    void fourOptionsAreAccepted() {
        assertThat(service.open(owner, stream.getId(), poll("?", "1", "2", "3", "4")).options()).hasSize(4);
    }

    @Test
    void questionOver150OrOptionOver60OrBlankIsRejected() {
        assertThatThrownBy(() -> service.open(owner, stream.getId(), poll("q".repeat(151), "a", "b")))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.open(owner, stream.getId(), poll("?", "o".repeat(61), "b")))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.open(owner, stream.getId(), poll("  ", "a", "b")))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.open(owner, stream.getId(), poll("?", "a", "  ")))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.BAD_REQUEST));

        // đúng giới hạn thì được
        assertThat(service.open(owner, stream.getId(), poll("q".repeat(150), "o".repeat(60), "b")).question()).hasSize(150);
    }

    @Test
    void cannotOpenAPollOnAnEndedStream() {
        stream.setStatus(LiveStatus.ENDED);

        assertThatThrownBy(() -> open()).satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void closeMarksItClosedBroadcastsAndIsIdempotent() {
        PollView view = open();
        UUID pollId = UUID.fromString(view.id());

        PollView closed = service.close(owner, stream.getId(), pollId);
        PollView again = service.close(owner, stream.getId(), pollId);

        assertThat(closed.open()).isFalse();
        assertThat(again.open()).isFalse();
        assertThat(pollEvents()).extracting(e -> e.getPoll().open()).containsExactly(true, false);
    }

    // ----------------------------------------------------------------- bình chọn

    @Test
    void voteRecordsTheChoiceAndReturnsMyVote() {
        PollView view = open();
        UUID pollId = UUID.fromString(view.id());
        when(votes.countByOption(pollId)).thenReturn(List.<Object[]>of(new Object[] {1, 1L}));

        PollView result = service.vote(fan, stream.getId(), pollId, 1);

        ArgumentCaptor<LivePollVote> saved = ArgumentCaptor.forClass(LivePollVote.class);
        verify(votes).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getId().getPollId()).isEqualTo(pollId);
        assertThat(saved.getValue().getId().getUserId()).isEqualTo(fan);
        assertThat(saved.getValue().getOptionIndex()).isEqualTo(1);
        assertThat(result.myVote()).isEqualTo(1);
        assertThat(result.counts()).containsExactly(0, 1, 0);
        assertThat(result.total()).isEqualTo(1);
    }

    @Test
    void votingTwiceIs409() {
        PollView view = open();
        UUID pollId = UUID.fromString(view.id());
        when(votes.existsById(new LivePollVoteId(pollId, fan))).thenReturn(true);

        assertThatThrownBy(() -> service.vote(fan, stream.getId(), pollId, 0))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.CONFLICT));
        verify(votes, never()).saveAndFlush(any());
    }

    @Test
    void aSecondVoteThatRacesInIs409TooBecauseThePrimaryKeyRejectsIt() {
        PollView view = open();
        doThrow(new DataIntegrityViolationException("duplicate")).when(votes).saveAndFlush(any(LivePollVote.class));

        assertThatThrownBy(() -> service.vote(fan, stream.getId(), UUID.fromString(view.id()), 0))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void votingOnAClosedPollIs409() {
        PollView view = open();
        UUID pollId = UUID.fromString(view.id());
        service.close(owner, stream.getId(), pollId);

        assertThatThrownBy(() -> service.vote(fan, stream.getId(), pollId, 0))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void anOptionOutsideTheRangeIs400() {
        PollView view = open();
        UUID pollId = UUID.fromString(view.id());

        assertThatThrownBy(() -> service.vote(fan, stream.getId(), pollId, 3))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.vote(fan, stream.getId(), pollId, -1))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void aPollOfAnotherStreamIs404() {
        PollView view = open();
        UUID otherStream = UUID.randomUUID();

        assertThatThrownBy(() -> service.vote(fan, otherStream, UUID.fromString(view.id()), 0))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void votingIsRateLimitedPerUser() {
        PollView view = open();
        doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "nhanh quá"))
                .when(rateLimiter).check(Limit.LIVE_VOTE, fan);

        assertThatThrownBy(() -> service.vote(fan, stream.getId(), UUID.fromString(view.id()), 0))
                .satisfies(e -> assertThat(statusOf(e)).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
        verify(votes, never()).saveAndFlush(any());
    }

    @Test
    void manyVotesAreBroadcastOnceWhenTheSecondTicksOver() {
        PollView view = open();
        UUID pollId = UUID.fromString(view.id());
        for (int i = 0; i < 50; i++) {
            service.vote(UUID.randomUUID(), stream.getId(), pollId, i % 3);
        }
        assertThat(pollEvents()).hasSize(1); // chỉ lần mở, vote chưa phát riêng

        service.flush();
        service.flush();

        assertThat(pollEvents()).hasSize(2);
    }

    // ------------------------------------------------------------------- đọc

    @Test
    void countsAndMyVoteAreInTheViewerSpecificView() {
        PollView view = open();
        UUID pollId = UUID.fromString(view.id());
        when(votes.countByOption(pollId)).thenReturn(List.of(new Object[] {0, 3L}, new Object[] {2, 1L}));
        when(votes.findById(new LivePollVoteId(pollId, fan)))
                .thenReturn(Optional.of(new LivePollVote(new LivePollVoteId(pollId, fan), 2, now)));

        PollView mine = service.current(stream.getId(), fan, now).orElseThrow();
        PollView guest = service.current(stream.getId(), null, now).orElseThrow();

        assertThat(mine.counts()).containsExactly(3, 0, 1);
        assertThat(mine.total()).isEqualTo(4);
        assertThat(mine.myVote()).isEqualTo(2);
        assertThat(guest.myVote()).isNull();
    }

    @Test
    void aJustClosedPollStaysVisibleFor60SecondsThenDisappears() {
        PollView view = open();
        LivePoll entity = stored.get(0);
        service.close(owner, stream.getId(), UUID.fromString(view.id()));
        entity.setClosedAt(now);

        assertThat(service.current(stream.getId(), null, now.plusSeconds(59))).isPresent();
        assertThat(service.current(stream.getId(), null, now.plusSeconds(61))).isEmpty();
    }

    @Test
    void historyListsEveryPollWithFinalCounts() {
        PollView first = open();
        service.close(owner, stream.getId(), UUID.fromString(first.id()));
        PollView second = service.open(owner, stream.getId(), poll("Hai?", "x", "y"));
        when(votes.countByOption(UUID.fromString(second.id()))).thenReturn(List.<Object[]>of(new Object[] {1, 5L}));

        List<PollView> history = service.history(stream.getId());

        assertThat(history).extracting(PollView::question).containsExactly("Chơi game nào?", "Hai?");
        assertThat(history.get(1).counts()).containsExactly(0, 5);
        assertThat(history).allSatisfy(p -> assertThat(p.myVote()).isNull());
    }

    @Test
    void endingTheStreamClosesTheOpenPoll() {
        open();

        service.onEnded(stream.getId());

        assertThat(stored.get(0).isOpen()).isFalse();
        assertThat(stored.get(0).getClosedAt()).isNotNull();
        verify(polls, times(2)).save(any(LivePoll.class));
    }
}
