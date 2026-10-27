package com.wetube.wetube_service.service.live;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wetube.wetube_service.dto.live.LiveEvent;
import com.wetube.wetube_service.dto.live.OpenPollRequest;
import com.wetube.wetube_service.dto.live.PollView;
import com.wetube.wetube_service.entity.live.LivePoll;
import com.wetube.wetube_service.entity.live.LivePollVote;
import com.wetube.wetube_service.entity.live.LivePollVoteId;
import com.wetube.wetube_service.entity.live.LiveStream;
import com.wetube.wetube_service.enumeration.LiveStatus;
import com.wetube.wetube_service.repository.live.LivePollRepository;
import com.wetube.wetube_service.repository.live.LivePollVoteRepository;
import com.wetube.wetube_service.repository.live.LiveStreamRepository;
import com.wetube.wetube_service.security.RateLimiter;
import com.wetube.wetube_service.security.RateLimiter.Limit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Poll của buổi phát trực tiếp: mỗi lúc một Poll mở, mỗi người chọn một lần (khoá chính của bảng phiếu chặn cả hai
 * yêu cầu đồng thời). Kết quả đẩy tới người xem tối đa một lần mỗi giây dù có bao nhiêu phiếu mới.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class LivePollService implements LiveCleanup {

    static final int QUESTION_MAX = 150;
    static final int OPTION_MAX = 60;
    static final int OPTIONS_MIN = 2;
    static final int OPTIONS_MAX = 4;
    private static final Duration CLOSED_VISIBLE = Duration.ofSeconds(60);

    private final LiveStreamRepository streams;
    private final LivePollRepository polls;
    private final LivePollVoteRepository votes;
    private final RateLimiter rateLimiter;
    private final LiveEventPublisher publisher;
    private final ObjectMapper mapper;

    /** Poll có phiếu mới chưa phát: pollId → streamId. */
    private final Map<UUID, UUID> dirty = new ConcurrentHashMap<>();

    public PollView open(UUID ownerUserId, UUID streamId, OpenPollRequest req) {
        LiveStream stream = ownedBy(ownerUserId, streamId);
        if (stream.getStatus() != LiveStatus.CREATED && stream.getStatus() != LiveStatus.LIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Buổi phát đã kết thúc");
        }
        String question = clean(req == null ? null : req.question(), QUESTION_MAX, "Câu hỏi");
        List<String> options = new ArrayList<>();
        List<String> raw = req == null || req.options() == null ? List.of() : req.options();
        if (raw.size() < OPTIONS_MIN || raw.size() > OPTIONS_MAX) {
            throw badRequest("Poll cần từ " + OPTIONS_MIN + " đến " + OPTIONS_MAX + " lựa chọn");
        }
        for (String option : raw) {
            options.add(clean(option, OPTION_MAX, "Lựa chọn"));
        }
        if (polls.findFirstByStreamIdAndOpenTrue(streamId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Hãy đóng Poll đang mở trước khi mở Poll mới");
        }
        LivePoll poll = polls.save(LivePoll.builder()
                .streamId(streamId)
                .question(question)
                .options(write(options))
                .open(true)
                .createdAt(LocalDateTime.now())
                .build());
        PollView view = view(poll, null);
        publisher.toStream(streamId, LiveEvent.builder().type("POLL").poll(view).build());
        return view;
    }

    public PollView close(UUID ownerUserId, UUID streamId, UUID pollId) {
        ownedBy(ownerUserId, streamId);
        LivePoll poll = pollOf(streamId, pollId);
        if (poll.isOpen()) {
            poll.setOpen(false);
            poll.setClosedAt(LocalDateTime.now());
            polls.save(poll);
            dirty.remove(pollId);
            publisher.toStream(streamId, LiveEvent.builder().type("POLL").poll(view(poll, null)).build());
        }
        return view(poll, null);
    }

    public PollView vote(UUID userId, UUID streamId, UUID pollId, int option) {
        rateLimiter.check(Limit.LIVE_VOTE, userId);
        LivePoll poll = pollOf(streamId, pollId);
        if (!poll.isOpen()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Poll đã đóng");
        }
        if (option < 0 || option >= read(poll).size()) {
            throw badRequest("Lựa chọn không hợp lệ");
        }
        LivePollVoteId id = new LivePollVoteId(pollId, userId);
        if (votes.existsById(id)) {
            throw alreadyVoted();
        }
        try {
            votes.saveAndFlush(new LivePollVote(id, option, LocalDateTime.now()));
        } catch (DataIntegrityViolationException e) {
            throw alreadyVoted();
        }
        dirty.put(pollId, streamId);
        return view(poll, option);
    }

    /** Poll đang mở, hoặc Poll vừa đóng trong 60 giây gần nhất; myVote theo người xem (null với khách). */
    @Transactional(readOnly = true)
    public Optional<PollView> current(UUID streamId, UUID viewerIdOrNull) {
        return current(streamId, viewerIdOrNull, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    Optional<PollView> current(UUID streamId, UUID viewerIdOrNull, LocalDateTime now) {
        Optional<LivePoll> open = polls.findFirstByStreamIdAndOpenTrue(streamId);
        if (open.isPresent()) {
            return open.map(p -> view(p, myVote(p.getId(), viewerIdOrNull)));
        }
        return polls.findByStreamIdOrderByCreatedAtAsc(streamId).stream()
                .filter(p -> p.getClosedAt() != null && p.getClosedAt().plus(CLOSED_VISIBLE).isAfter(now))
                .reduce((first, second) -> second)
                .map(p -> view(p, myVote(p.getId(), viewerIdOrNull)));
    }

    /** Mọi Poll của buổi theo thứ tự tạo, kèm kết quả cuối (trang "đã kết thúc"). */
    @Transactional(readOnly = true)
    public List<PollView> history(UUID streamId) {
        return polls.findByStreamIdOrderByCreatedAtAsc(streamId).stream().map(p -> view(p, null)).toList();
    }

    @Override
    public void onEnded(UUID streamId) {
        polls.findFirstByStreamIdAndOpenTrue(streamId).ifPresent(poll -> {
            poll.setOpen(false);
            poll.setClosedAt(LocalDateTime.now());
            polls.save(poll);
        });
        dirty.values().removeIf(streamId::equals);
    }

    /** Mỗi giây: phát kết quả mới của các Poll vừa có phiếu (gộp mọi phiếu trong giây đó thành một tin). */
    @Scheduled(fixedDelay = 1000)
    @Transactional(readOnly = true)
    public void flush() {
        for (Map.Entry<UUID, UUID> entry : Map.copyOf(dirty).entrySet()) {
            dirty.remove(entry.getKey(), entry.getValue());
            try {
                polls.findById(entry.getKey()).ifPresent(poll -> publisher.toStream(entry.getValue(),
                        LiveEvent.builder().type("POLL").poll(view(poll, null)).build()));
            } catch (RuntimeException e) {
                log.warn("[Live] Không phát được kết quả Poll {}: {}", entry.getKey(), e.getMessage());
            }
        }
    }

    // ------------------------------------------------------------ nội bộ

    private LiveStream ownedBy(UUID userId, UUID streamId) {
        LiveStream stream = streams.findById(streamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy buổi phát"));
        if (!stream.getOwnerId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ chủ kênh mới quản lý được Poll");
        }
        return stream;
    }

    private LivePoll pollOf(UUID streamId, UUID pollId) {
        return polls.findById(pollId).filter(p -> p.getStreamId().equals(streamId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy Poll"));
    }

    private Integer myVote(UUID pollId, UUID viewerIdOrNull) {
        if (viewerIdOrNull == null) {
            return null;
        }
        return votes.findById(new LivePollVoteId(pollId, viewerIdOrNull)).map(LivePollVote::getOptionIndex).orElse(null);
    }

    private PollView view(LivePoll poll, Integer myVote) {
        List<String> options = read(poll);
        int[] counts = new int[options.size()];
        for (Object[] row : votes.countByOption(poll.getId())) {
            int index = ((Number) row[0]).intValue();
            if (index >= 0 && index < counts.length) {
                counts[index] = ((Number) row[1]).intValue();
            }
        }
        List<Integer> countList = new ArrayList<>();
        int total = 0;
        for (int count : counts) {
            countList.add(count);
            total += count;
        }
        return new PollView(poll.getId().toString(), poll.getQuestion(), options, countList, total, poll.isOpen(), myVote);
    }

    private String write(List<String> options) {
        try {
            return mapper.writeValueAsString(options);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<String> read(LivePoll poll) {
        try {
            return mapper.readValue(poll.getOptions(), new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Poll " + poll.getId() + " có lựa chọn không đọc được", e);
        }
    }

    private static String clean(String raw, int max, String what) {
        String text = raw == null ? "" : raw.strip();
        if (text.isEmpty()) {
            throw badRequest(what + " không được để trống");
        }
        if (text.length() > max) {
            throw badRequest(what + " tối đa " + max + " ký tự");
        }
        return text;
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static ResponseStatusException alreadyVoted() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Bạn đã bình chọn rồi");
    }
}
