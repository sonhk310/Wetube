package com.wetube.wetube_service.service.live;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.wetube.wetube_service.dto.live.ChannelLivesResponse;
import com.wetube.wetube_service.dto.live.CreateLiveRequest;
import com.wetube.wetube_service.dto.live.IngressView;
import com.wetube.wetube_service.dto.live.LiveCard;
import com.wetube.wetube_service.dto.live.LiveEvent;
import com.wetube.wetube_service.dto.live.LiveTokenResponse;
import com.wetube.wetube_service.dto.live.LiveView;
import com.wetube.wetube_service.dto.live.PollView;
import com.wetube.wetube_service.dto.live.UpdateLiveRequest;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.channel.Channel;
import com.wetube.wetube_service.entity.live.LiveStream;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.enumeration.LiveProtocol;
import com.wetube.wetube_service.enumeration.LiveSource;
import com.wetube.wetube_service.enumeration.LiveStatus;
import com.wetube.wetube_service.repository.UserRepository;
import com.wetube.wetube_service.repository.channel.ChannelRepository;
import com.wetube.wetube_service.repository.live.LiveStreamRepository;
import com.wetube.wetube_service.security.RateLimiter;
import com.wetube.wetube_service.security.RateLimiter.Limit;
import com.wetube.wetube_service.service.live.LiveKitGateway.IngressDetails;
import com.wetube.wetube_service.service.live.LiveKitGateway.Participant;
import com.wetube.wetube_service.service.notification.NotificationService;

import lombok.extern.slf4j.Slf4j;

/**
 * Buổi phát trực tiếp: tạo, sửa, kết thúc và đọc. Danh tính luôn là người đăng nhập (không bao giờ lấy từ nội dung
 * request). Mọi lời gọi LiveKit đi qua {@link LiveKitGateway}.
 */
@Slf4j
@Service
@Transactional
public class LiveStreamService {

    static final int TITLE_MAX = 100;
    static final int DESCRIPTION_MAX = 5000;
    static final int VIEWER_CAP = 40;
    private static final Duration GRACE = Duration.ofMinutes(2);
    private static final Duration CREATED_TTL = Duration.ofHours(1);
    private static final Duration MAX_LIFE = Duration.ofHours(12);
    private static final List<LiveStatus> UNFINISHED = List.of(LiveStatus.CREATED, LiveStatus.LIVE);

    private final LiveStreamRepository repo;
    private final UserRepository userRepo;
    private final ChannelRepository channelRepo;
    private final LiveKitGateway gateway;
    private final RateLimiter rateLimiter;
    private final LiveEventPublisher publisher;
    private final NotificationService notifications;
    private final LivePollService polls;
    private final List<LiveCleanup> cleanups;

    /** Một kênh chỉ tạo một buổi tại một thời điểm (backend chạy một tiến trình). */
    private final Map<UUID, Object> channelLocks = new ConcurrentHashMap<>();

    public LiveStreamService(LiveStreamRepository repo, UserRepository userRepo, ChannelRepository channelRepo,
                             LiveKitGateway gateway, RateLimiter rateLimiter, LiveEventPublisher publisher,
                             NotificationService notifications, LivePollService polls, List<LiveCleanup> cleanups) {
        this.repo = repo;
        this.userRepo = userRepo;
        this.channelRepo = channelRepo;
        this.gateway = gateway;
        this.rateLimiter = rateLimiter;
        this.publisher = publisher;
        this.notifications = notifications;
        this.polls = polls;
        this.cleanups = cleanups;
    }

    // ------------------------------------------------------------------ ghi

    public LiveView create(UUID userId, CreateLiveRequest req) {
        String title = cleanTitle(req.title());
        String description = cleanDescription(req.description());
        LiveSource source = req.source() == null ? LiveSource.BROWSER : req.source();

        AppUser user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Hãy đăng nhập để phát trực tiếp"));
        Channel channel = user.getChannel();
        if (channel == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn cần có kênh để phát trực tiếp");
        }
        if (channel.getStatus() == ActiveStatus.INACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Kênh của bạn đang bị ẩn nên không thể phát trực tiếp");
        }
        rateLimiter.check(Limit.LIVE_CREATE, userId);

        synchronized (channelLocks.computeIfAbsent(channel.getId(), k -> new Object())) {
            repo.findFirstByChannelIdAndStatusIn(channel.getId(), UNFINISHED).ifPresent(existing -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Kênh đang có một buổi phát chưa kết thúc (" + existing.getId() + ")");
            });

            String room = "live-" + UUID.randomUUID();
            LiveProtocol protocol = source == LiveSource.ENCODER
                    ? (req.protocol() == null ? LiveProtocol.WHIP : req.protocol())
                    : null;
            IngressDetails ingress = null;
            try {
                gateway.createRoom(room, "{\"ownerId\":\"" + userId + "\"}");
                if (source == LiveSource.ENCODER) {
                    ingress = gateway.createIngress(room, identityOf(userId), channel.getName(), protocol);
                }
            } catch (LiveKitUnavailableException e) {
                deleteRoomQuietly(room);
                throw unavailable(e);
            }

            LiveStream saved = repo.save(LiveStream.builder()
                    .channelId(channel.getId())
                    .ownerId(userId)
                    .title(title)
                    .description(description)
                    .source(source)
                    .protocol(protocol)
                    .status(LiveStatus.CREATED)
                    .chatEnabled(req.chatEnabled() == null || req.chatEnabled())
                    .livekitRoom(room)
                    .ingressId(ingress == null ? null : ingress.ingressId())
                    .ingressUrl(ingress == null ? null : ingress.url())
                    .ingressKey(ingress == null ? null : ingress.streamKey())
                    .createdAt(LocalDateTime.now())
                    .build());
            return view(saved, channel, true);
        }
    }

    public LiveView update(UUID userId, UUID id, UpdateLiveRequest req) {
        LiveStream stream = owned(userId, id);
        if (isFinished(stream)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Buổi phát đã kết thúc");
        }
        if (req.title() != null) {
            stream.setTitle(cleanTitle(req.title()));
        }
        if (req.description() != null) {
            stream.setDescription(cleanDescription(req.description()));
        }
        boolean chatChanged = req.chatEnabled() != null && req.chatEnabled() != stream.isChatEnabled();
        if (chatChanged) {
            stream.setChatEnabled(req.chatEnabled());
        }
        repo.save(stream);
        if (chatChanged) {
            publisher.toStream(id, LiveEvent.builder().type("CHAT_STATE").chatEnabled(stream.isChatEnabled()).build());
        }
        return view(stream, channelOf(stream.getChannelId()), true);
    }

    /** Chủ kênh hoặc admin kết thúc buổi phát; gọi lại khi đã kết thúc thì không làm gì. */
    public void end(UUID userId, boolean admin, UUID id) {
        LiveStream stream = find(id);
        if (!admin && !stream.getOwnerId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ chủ kênh mới kết thúc được buổi phát");
        }
        if (isFinished(stream)) {
            return;
        }
        finish(stream, LiveStatus.ENDED, LocalDateTime.now());
    }

    /**
     * Đưa buổi về ENDED / CANCELED: xoá phòng và Ingress (LiveKit lỗi thì vẫn kết thúc), ghi giờ, báo người xem,
     * rồi cho các LiveCleanup (chat, Poll) dọn.
     */
    void finish(LiveStream stream, LiveStatus status, LocalDateTime now) {
        if (stream.getLivekitRoom() != null) {
            deleteRoomQuietly(stream.getLivekitRoom());
        }
        if (stream.getIngressId() != null) {
            try {
                gateway.deleteIngress(stream.getIngressId());
            } catch (LiveKitUnavailableException e) {
                log.warn("[Live] Không xoá được Ingress {}: {}", stream.getIngressId(), e.getMessage());
            }
        }
        stream.setStatus(status);
        stream.setEndedAt(now);
        stream.setCurrentViewers(0);
        stream.setIngressId(null);
        stream.setIngressUrl(null);
        stream.setIngressKey(null);
        repo.save(stream);
        publisher.toStream(stream.getId(), LiveEvent.builder().type("STATUS").status(status.name()).build());
        for (LiveCleanup cleanup : cleanups) {
            try {
                cleanup.onEnded(stream.getId());
            } catch (RuntimeException e) {
                log.warn("[Live] Dọn buổi {} lỗi: {}", stream.getId(), e.getMessage());
            }
        }
    }

    // ---------------------------------------------- token và vòng đời theo thời gian

    /**
     * Token vào phòng LiveKit. Chủ kênh phát bằng webcam nhận token phát; mọi người khác (kể cả chủ kênh khi phát
     * bằng OBS) nhận token xem với identity ngẫu nhiên. Đủ {@value #VIEWER_CAP} người xem thì {@code full}.
     */
    public LiveTokenResponse token(UUID id, UUID viewerIdOrNull) {
        LiveStream stream = find(id);
        if (stream.getStatus() == LiveStatus.CANCELED) {
            throw notFound();
        }
        if (stream.getStatus() == LiveStatus.ENDED) {
            throw new ResponseStatusException(HttpStatus.GONE, "Buổi phát đã kết thúc");
        }
        boolean isOwner = viewerIdOrNull != null && viewerIdOrNull.equals(stream.getOwnerId());
        try {
            String room = stream.getLivekitRoom();
            if (isOwner && stream.getSource() == LiveSource.BROWSER) {
                Channel channel = channelOf(stream.getChannelId());
                return new LiveTokenResponse(
                        gateway.publisherToken(room, identityOf(stream.getOwnerId()),
                                channel == null ? null : channel.getName()),
                        gateway.wsUrl(), "PUBLISHER", false);
            }
            if (countViewers(gateway.participants(room)) >= VIEWER_CAP) {
                return new LiveTokenResponse(null, gateway.wsUrl(), "VIEWER", true);
            }
            return new LiveTokenResponse(gateway.viewerToken(room, "v:" + UUID.randomUUID()), gateway.wsUrl(),
                    "VIEWER", false);
        } catch (LiveKitUnavailableException e) {
            throw unavailable(e);
        }
    }

    /** Chủ kênh báo đã đăng luồng webcam: chuyển LIVE ngay nếu LiveKit thấy hình, không thì để job 15 giây lo. */
    public void markStarted(UUID userId, UUID id) {
        LiveStream stream = owned(userId, id);
        if (stream.getStatus() != LiveStatus.CREATED) {
            return;
        }
        try {
            if (gateway.participants(stream.getLivekitRoom()).stream().anyMatch(Participant::publishingVideo)) {
                promote(stream, LocalDateTime.now());
            }
        } catch (LiveKitUnavailableException e) {
            throw unavailable(e);
        }
    }

    /**
     * Một nhịp của job 15 giây: hỏi LiveKit từng buổi chưa kết thúc để chuyển CREATED → LIVE, cập nhật số người xem
     * và tự kết thúc theo thời gian. Một buổi lỗi không làm các buổi khác dừng.
     */
    public void refreshAll(LocalDateTime now) {
        for (LiveStream stream : repo.findByStatusIn(UNFINISHED)) {
            try {
                refresh(stream, now);
            } catch (RuntimeException e) {
                log.warn("[Live] Cập nhật buổi {} lỗi: {}", stream.getId(), e.getMessage());
            }
        }
    }

    private void refresh(LiveStream stream, LocalDateTime now) {
        if (stream.getStatus() == LiveStatus.CREATED && stream.getCreatedAt().plus(CREATED_TTL).isBefore(now)) {
            finish(stream, LiveStatus.CANCELED, now);
            return;
        }
        if (stream.getStatus() == LiveStatus.LIVE && stream.getStartedAt() != null
                && stream.getStartedAt().plus(MAX_LIFE).isBefore(now)) {
            finish(stream, LiveStatus.ENDED, now);
            return;
        }
        // LiveKit lỗi thì ném ra ngoài: không được coi là "mất người phát"
        List<Participant> participants = gateway.participants(stream.getLivekitRoom());
        boolean publisherPresent = participants.stream().anyMatch(Participant::publishingVideo);
        int viewers = (int) countViewers(participants);

        if (stream.getStatus() == LiveStatus.CREATED) {
            if (publisherPresent) {
                promote(stream, now);
            }
        } else if (publisherPresent) {
            stream.setLastPublisherAt(now);
        } else {
            LocalDateTime last = stream.getLastPublisherAt() != null ? stream.getLastPublisherAt() : stream.getStartedAt();
            if (last != null && last.plus(GRACE).isBefore(now)) {
                finish(stream, LiveStatus.ENDED, now);
                return;
            }
        }

        boolean viewersChanged = viewers != stream.getCurrentViewers();
        stream.setCurrentViewers(viewers);
        stream.setPeakViewers(Math.max(stream.getPeakViewers(), viewers));
        repo.save(stream);
        if (viewersChanged) {
            publisher.toStream(stream.getId(), LiveEvent.builder().type("VIEWERS").viewers(viewers).build());
            log.info("[Live] Buổi {} có {} người xem (cao nhất {})", stream.getId(), viewers, stream.getPeakViewers());
        }
    }

    /** CREATED → LIVE: ghi giờ bắt đầu, báo người xem và người đăng ký (một lần cho mỗi buổi). */
    private void promote(LiveStream stream, LocalDateTime now) {
        stream.setStatus(LiveStatus.LIVE);
        stream.setStartedAt(now);
        stream.setLastPublisherAt(now);
        repo.save(stream);
        publisher.toStream(stream.getId(), LiveEvent.builder().type("STATUS").status(LiveStatus.LIVE.name()).build());
        try {
            notifications.notifyLiveStarted(stream.getId(), stream.getOwnerId(), stream.getChannelId(),
                    stream.getTitle());
        } catch (RuntimeException e) {
            log.warn("[Live] Không gửi được thông báo cho buổi {}: {}", stream.getId(), e.getMessage());
        }
    }

    /** Xoá Ingress không dùng trên LiveKit, giữ Ingress của mọi buổi chưa kết thúc. */
    public int cleanInactiveIngress() {
        Set<String> keep = repo.findByStatusIn(UNFINISHED).stream()
                .map(LiveStream::getIngressId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        try {
            return gateway.deleteInactiveIngress(keep);
        } catch (LiveKitUnavailableException e) {
            log.warn("[Live] Không dọn được Ingress: {}", e.getMessage());
            return 0;
        }
    }

    private static long countViewers(List<Participant> participants) {
        return participants.stream().filter(p -> p.identity().startsWith("v:")).count();
    }

    // ------------------------------------------------------------------ đọc

    /** Buổi chưa kết thúc của kênh người gọi, kèm thông tin Ingress. */
    @Transactional(readOnly = true)
    public Optional<LiveView> mine(UUID userId) {
        Channel channel = userRepo.findById(userId).map(AppUser::getChannel).orElse(null);
        if (channel == null) {
            return Optional.empty();
        }
        return repo.findFirstByChannelIdAndStatusIn(channel.getId(), UNFINISHED)
                .map(stream -> view(stream, channel, true));
    }

    @Transactional(readOnly = true)
    public LiveView get(UUID id, UUID viewerIdOrNull) {
        LiveStream stream = find(id);
        if (stream.getStatus() == LiveStatus.CANCELED) {
            throw notFound();
        }
        boolean isOwner = viewerIdOrNull != null && viewerIdOrNull.equals(stream.getOwnerId());
        LiveView base = view(stream, channelOf(stream.getChannelId()), isOwner);
        // Đang phát: Poll hiện tại; đã kết thúc: mọi Poll với kết quả cuối
        if (stream.getStatus() == LiveStatus.ENDED) {
            return withPolls(base, null, polls.history(id));
        }
        return withPolls(base, polls.current(id, viewerIdOrNull).orElse(null), null);
    }

    private static LiveView withPolls(LiveView base, PollView poll, List<PollView> history) {
        return LiveView.builder()
                .id(base.id()).channelId(base.channelId()).channelName(base.channelName())
                .channelPicture(base.channelPicture()).channelHandle(base.channelHandle()).ownerId(base.ownerId())
                .title(base.title()).description(base.description()).status(base.status()).source(base.source())
                .protocol(base.protocol()).chatEnabled(base.chatEnabled()).currentViewers(base.currentViewers())
                .peakViewers(base.peakViewers()).createdAt(base.createdAt()).startedAt(base.startedAt())
                .endedAt(base.endedAt()).ingress(base.ingress()).poll(poll).polls(history)
                .build();
    }

    @Transactional(readOnly = true)
    public List<LiveCard> listLive() {
        return cards(repo.findByStatusOrderByCurrentViewersDesc(LiveStatus.LIVE));
    }

    @Transactional(readOnly = true)
    public ChannelLivesResponse byChannel(UUID channelId, Pageable pageable) {
        LiveCard current = repo.findFirstByChannelIdAndStatusIn(channelId, List.of(LiveStatus.LIVE))
                .map(s -> cards(List.of(s)).get(0))
                .orElse(null);
        Page<LiveStream> past = repo.findByChannelIdAndStatusOrderByEndedAtDesc(channelId, LiveStatus.ENDED, pageable);
        return new ChannelLivesResponse(current, cards(past.getContent()), past.getTotalElements(), past.hasNext());
    }

    // ------------------------------------------------------------ nội bộ

    LiveStream find(UUID id) {
        return repo.findById(id).orElseThrow(LiveStreamService::notFound);
    }

    private LiveStream owned(UUID userId, UUID id) {
        LiveStream stream = find(id);
        if (!stream.getOwnerId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ chủ kênh mới sửa được buổi phát");
        }
        return stream;
    }

    static boolean isFinished(LiveStream stream) {
        return stream.getStatus() == LiveStatus.ENDED || stream.getStatus() == LiveStatus.CANCELED;
    }

    static String identityOf(UUID ownerId) {
        return "u:" + ownerId;
    }

    private Channel channelOf(UUID channelId) {
        return channelRepo.findById(channelId).orElse(null);
    }

    private List<LiveCard> cards(List<LiveStream> streams) {
        Map<UUID, Channel> channels = new HashMap<>();
        channelRepo.findAllById(streams.stream().map(LiveStream::getChannelId).distinct().toList())
                .forEach(c -> channels.put(c.getId(), c));
        return streams.stream().map(s -> {
            Channel c = channels.get(s.getChannelId());
            return new LiveCard(s.getId(), s.getTitle(), s.getChannelId(),
                    c == null ? null : c.getName(), c == null ? null : c.getPicture(),
                    c == null ? null : c.getBackgroundImgUrl(),
                    s.getCurrentViewers(), s.getPeakViewers(), s.getStartedAt(), s.getEndedAt(), s.getStatus());
        }).toList();
    }

    private LiveView view(LiveStream s, Channel channel, boolean includeIngress) {
        boolean hasIngress = includeIngress && s.getIngressUrl() != null;
        return LiveView.builder()
                .id(s.getId())
                .channelId(s.getChannelId())
                .channelName(channel == null ? null : channel.getName())
                .channelPicture(channel == null ? null : channel.getPicture())
                .channelHandle(channel == null ? null : channel.getHandle())
                .ownerId(s.getOwnerId())
                .title(s.getTitle())
                .description(s.getDescription())
                .status(s.getStatus())
                .source(s.getSource())
                .protocol(s.getProtocol())
                .chatEnabled(s.isChatEnabled())
                .currentViewers(s.getCurrentViewers())
                .peakViewers(s.getPeakViewers())
                .createdAt(s.getCreatedAt())
                .startedAt(s.getStartedAt())
                .endedAt(s.getEndedAt())
                .ingress(hasIngress ? new IngressView(s.getIngressUrl(), s.getIngressKey()) : null)
                .build();
    }

    private void deleteRoomQuietly(String room) {
        try {
            gateway.deleteRoom(room);
        } catch (LiveKitUnavailableException e) {
            log.warn("[Live] Không xoá được phòng {}: {}", room, e.getMessage());
        }
    }

    static ResponseStatusException unavailable(LiveKitUnavailableException e) {
        log.warn("[Live] LiveKit không dùng được (quota={}): {}", e.isQuota(), e.getMessage());
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.isQuota()
                ? "Đã hết hạn mức phát trực tiếp của tháng này, sẽ có lại vào ngày 1"
                : "Không kết nối được máy chủ phát trực tiếp, vui lòng thử lại");
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy buổi phát");
    }

    private static String cleanTitle(String raw) {
        String title = raw == null ? "" : raw.strip();
        if (title.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hãy nhập tiêu đề buổi phát");
        }
        if (title.length() > TITLE_MAX) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tiêu đề tối đa " + TITLE_MAX + " ký tự");
        }
        return title;
    }

    private static String cleanDescription(String raw) {
        if (raw == null) {
            return null;
        }
        String description = raw.strip();
        if (description.length() > DESCRIPTION_MAX) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mô tả tối đa " + DESCRIPTION_MAX + " ký tự");
        }
        return description;
    }
}
