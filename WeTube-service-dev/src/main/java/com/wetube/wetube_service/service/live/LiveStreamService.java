package com.wetube.wetube_service.service.live;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.wetube.wetube_service.dto.LiveRecordingProperties;
import com.wetube.wetube_service.dto.live.ChannelLivesResponse;
import com.wetube.wetube_service.dto.live.CreateLiveRequest;
import com.wetube.wetube_service.dto.live.IngressView;
import com.wetube.wetube_service.dto.live.LiveCard;
import com.wetube.wetube_service.dto.live.LiveEvent;
import com.wetube.wetube_service.dto.live.LiveRecordingInfo;
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
import com.wetube.wetube_service.enumeration.RecordingStatus;
import com.wetube.wetube_service.repository.UserRepository;
import com.wetube.wetube_service.repository.channel.ChannelRepository;
import com.wetube.wetube_service.repository.live.LiveStreamRepository;
import com.wetube.wetube_service.security.RateLimiter;
import com.wetube.wetube_service.security.RateLimiter.Limit;
import com.wetube.wetube_service.service.CloudinaryService;
import com.wetube.wetube_service.service.CloudinaryService.UploadedVideo;
import com.wetube.wetube_service.service.live.LiveKitGateway.IngressDetails;
import com.wetube.wetube_service.service.live.LiveKitGateway.Participant;
import com.wetube.wetube_service.service.notification.NotificationService;
import com.wetube.wetube_service.service.video.TopicCatalog;
import com.wetube.wetube_service.service.video.VideoService;
import com.wetube.wetube_service.service.video.VideoService.LiveReplay;

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
    static final long THUMBNAIL_MAX_BYTES = 2L * 1024 * 1024;
    static final int VIEWER_CAP = 40;
    /** Trần cứng của phòng LiveKit: người xem + chủ kênh (webcam hoặc OBS) + chỗ dự phòng. */
    static final int ROOM_PARTICIPANT_CAP = VIEWER_CAP + 5;
    /** Bản ghi ngắn hơn mức này không đăng thành video. */
    static final int MIN_REPLAY_SECONDS = 30;
    /** Dừng ghi rồi mà trình duyệt chưa tải bản ghi lên sau chừng này thì thôi, đánh dấu lỗi. */
    static final Duration UPLOAD_WAIT = Duration.ofHours(6);
    private static final int MESSAGE_MAX = 200;
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter REPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
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
    private final CloudinaryService cloudinary;
    private final LiveRecordingProperties recording;
    private final TopicCatalog topicCatalog;
    private final VideoService videos;
    private final List<LiveCleanup> cleanups;
    /** Mỗi buổi được làm mới trong một transaction ngắn riêng (job 15 giây). */
    private final TransactionTemplate tx;
    /** Kiểm tra + lưu buổi mới commit ngay trong khoá của kênh. */
    private final TransactionTemplate txNew;

    /** Một kênh chỉ tạo một buổi tại một thời điểm (backend chạy một tiến trình). */
    private final Map<UUID, Object> channelLocks = new ConcurrentHashMap<>();
    /** Buổi đang có bản ghi được tải lên: lần Thử lại chồng lên lần đang chạy thì từ chối, không tạo hai video. */
    private final Set<UUID> uploading = ConcurrentHashMap.newKeySet();

    public LiveStreamService(LiveStreamRepository repo, UserRepository userRepo, ChannelRepository channelRepo,
                             LiveKitGateway gateway, RateLimiter rateLimiter, LiveEventPublisher publisher,
                             NotificationService notifications, LivePollService polls, CloudinaryService cloudinary,
                             LiveRecordingProperties recording, TopicCatalog topicCatalog, VideoService videos,
                             List<LiveCleanup> cleanups, PlatformTransactionManager transactionManager) {
        this.repo = repo;
        this.userRepo = userRepo;
        this.channelRepo = channelRepo;
        this.gateway = gateway;
        this.rateLimiter = rateLimiter;
        this.publisher = publisher;
        this.notifications = notifications;
        this.polls = polls;
        this.cloudinary = cloudinary;
        this.recording = recording;
        this.topicCatalog = topicCatalog;
        this.videos = videos;
        this.cleanups = cleanups;
        this.tx = new TransactionTemplate(transactionManager);
        this.txNew = new TransactionTemplate(transactionManager);
        this.txNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    // ------------------------------------------------------------------ ghi

    public LiveView create(UUID userId, CreateLiveRequest req) {
        String title = cleanTitle(req.title());
        String description = cleanDescription(req.description());
        LiveSource source = req.source() == null ? LiveSource.BROWSER : req.source();
        boolean wantsRecording = Boolean.TRUE.equals(req.recordingEnabled());
        String recordingTopic = wantsRecording ? cleanRecordingTopic(source, req.topicId()) : null;

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
        if (wantsRecording) {
            RecordingLeft left = recordingLeft(channel.getId(), LocalDateTime.now());
            if (left.seconds() <= 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, exhaustedMessage(left));
            }
        }
        rateLimiter.check(Limit.LIVE_CREATE, userId);

        // Kiểm tra "mỗi kênh một buổi" + INSERT chạy trong transaction riêng và commit trước khi nhả khoá. Nếu chạy trong
        // transaction ngoài thì commit đến sau khi nhả khoá, yêu cầu thứ hai không thấy dòng của yêu cầu thứ nhất.
        synchronized (channelLocks.computeIfAbsent(channel.getId(), k -> new Object())) {
            return txNew.execute(status -> {
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
                    gateway.createRoom(room, "{\"ownerId\":\"" + userId + "\"}", ROOM_PARTICIPANT_CAP);
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
                        .recordingEnabled(wantsRecording)
                        .topicId(recordingTopic)
                        .recordingStatus(RecordingStatus.NONE)
                        .livekitRoom(room)
                        .ingressId(ingress == null ? null : ingress.ingressId())
                        .ingressUrl(ingress == null ? null : ingress.url())
                        .ingressKey(ingress == null ? null : ingress.streamKey())
                        .createdAt(LocalDateTime.now())
                        .build());
                return view(saved, channel, true);
            });
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

    /** Chủ kênh đặt / đổi ảnh thu nhỏ khi buổi chưa kết thúc; ảnh cũ bị xoá trên Cloudinary sau khi lưu xong. */
    public LiveView uploadThumbnail(UUID userId, UUID id, MultipartFile file) {
        LiveStream stream = owned(userId, id);
        if (isFinished(stream)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Buổi phát đã kết thúc");
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hãy chọn một ảnh");
        }
        String type = file.getContentType();
        if (!"image/png".equals(type) && !"image/jpeg".equals(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ảnh thu nhỏ phải là PNG hoặc JPEG");
        }
        if (file.getSize() > THUMBNAIL_MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ảnh quá lớn (tối đa " + (THUMBNAIL_MAX_BYTES / 1024 / 1024) + " MB)");
        }
        rateLimiter.check(Limit.LIVE_THUMBNAIL, userId);

        String replaced = stream.getThumbnailUrl();
        stream.setThumbnailUrl(cloudinary.uploadThumbnail(file, false));
        repo.save(stream);
        deleteImageAfterCommit(replaced);
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
        // Trình duyệt dừng ghi rồi tải bản ghi lên sau (buổi ENDED vẫn có recording_status STOPPING tới lúc đó)
        if (recStatus(stream) == RecordingStatus.RECORDING) {
            stopRecording(stream, now, null);
        }
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
        // Buổi bị huỷ không vào lịch sử nên không cần giữ ảnh thu nhỏ; buổi đã phát thì giữ
        String canceledThumbnail = status == LiveStatus.CANCELED ? stream.getThumbnailUrl() : null;
        if (canceledThumbnail != null) {
            stream.setThumbnailUrl(null);
        }
        repo.save(stream);
        deleteImageAfterCommit(canceledThumbnail);
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
     * Token vào phòng LiveKit. {@code publish}: trang phát của chủ kênh (webcam) xin token phát; mọi trường hợp khác,
     * kể cả chủ kênh mở trang xem, nhận token xem với identity ngẫu nhiên. Đủ {@value #VIEWER_CAP} người xem thì
     * {@code full}.
     */
    public LiveTokenResponse token(UUID id, UUID viewerIdOrNull, boolean publish) {
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
            // Token phát (identity u:<owner>) chỉ cấp khi trang phát xin rõ. Trang xem của chính chủ kênh cũng dùng
            // token xem: nếu cùng identity thì LiveKit ngắt trang phát (DUPLICATE_IDENTITY) và buổi phát đứt.
            if (publish) {
                if (!isOwner) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ chủ kênh mới phát được buổi này");
                }
                if (stream.getSource() != LiveSource.BROWSER) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Buổi phát này dùng phần mềm phát, không phát bằng trình duyệt");
                }
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
            if (gateway.participants(stream.getLivekitRoom()).stream().anyMatch(Participant::publishing)) {
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
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void refreshAll(LocalDateTime now) {
        for (LiveStream listed : repo.findByStatusIn(UNFINISHED)) {
            UUID id = listed.getId();
            try {
                // Đọc lại trong transaction riêng của buổi này: buổi vừa được chủ kênh kết thúc / sửa sau khi danh sách
                // được đọc thì bỏ qua, không ghi đè bản cũ. Xung đột còn lại do @Version chặn (nhịp sau làm lại).
                tx.executeWithoutResult(status -> repo.findById(id)
                        .filter(current -> !isFinished(current))
                        .ifPresent(current -> refresh(current, now)));
            } catch (RuntimeException e) {
                log.warn("[Live] Cập nhật buổi {} lỗi: {}", id, e.getMessage());
            }
        }
        // Buổi đã dừng ghi (kể cả buổi đã kết thúc, không còn trong danh sách trên): chờ trình duyệt tải bản ghi lên
        for (LiveStream listed : repo.findByRecordingStatusIn(List.of(RecordingStatus.STOPPING))) {
            UUID id = listed.getId();
            try {
                tx.executeWithoutResult(status -> repo.findById(id)
                        .filter(current -> recStatus(current) == RecordingStatus.STOPPING)
                        .ifPresent(current -> giveUpWaitingForUpload(current, now)));
            } catch (RuntimeException e) {
                log.warn("[Live] Lấy bản ghi của buổi {} lỗi: {}", id, e.getMessage());
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
        boolean publisherPresent = participants.stream().anyMatch(Participant::publishing);
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

        refreshRecording(stream, now);

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
        if (stream.isRecordingEnabled()) {
            startRecording(stream, now);
        }
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

    // ------------------------------------------------------------------ ghi hình

    /** Form chuẩn bị hỏi: máy chủ có bật ghi hình không và kênh của người gọi còn bao nhiêu giây trong tháng. */
    @Transactional(readOnly = true)
    public LiveRecordingInfo recordingInfo(UUID userId) {
        if (!recording.isEnabled()) {
            return new LiveRecordingInfo(false, 0, null);
        }
        Channel channel = userRepo.findById(userId).map(AppUser::getChannel).orElse(null);
        if (channel == null) {
            return new LiveRecordingInfo(false, 0, null);
        }
        RecordingLeft left = recordingLeft(channel.getId(), LocalDateTime.now());
        return new LiveRecordingInfo(true, left.seconds(), left.limitedBy());
    }

    private static RecordingStatus recStatus(LiveStream stream) {
        return stream.getRecordingStatus() == null ? RecordingStatus.NONE : stream.getRecordingStatus();
    }

    /** Hợp lệ khi tạo buổi: chỉ buổi từ trình duyệt, máy chủ đã bật ghi hình, có chủ đề hợp lệ. Trả chủ đề đã làm sạch. */
    private String cleanRecordingTopic(LiveSource source, String raw) {
        if (source != LiveSource.BROWSER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chỉ ghi hình được buổi phát từ trình duyệt; với OBS hãy dùng chức năng ghi hình của OBS");
        }
        if (!recording.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ghi hình chưa được bật trên máy chủ");
        }
        String topic = raw == null ? "" : raw.strip();
        if (topic.isEmpty() || !topicCatalog.exists(topic)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hãy chọn chủ đề cho video xem lại");
        }
        return topic;
    }

    /** Số giây còn được ghi trong tháng (mức thấp hơn giữa phần của kênh và phần chung) và nơi bị giới hạn. */
    private record RecordingLeft(long seconds, String limitedBy) {
    }

    private RecordingLeft recordingLeft(UUID channelId, LocalDateTime now) {
        LocalDateTime from = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime to = from.plusMonths(1);
        List<LiveStream> running = repo.findByRecordingStatusIn(List.of(RecordingStatus.RECORDING));
        long channelLeft = recording.getChannelMinutes() * 60L
                - repo.sumRecordingSecondsByChannel(channelId, from, to) - runningSeconds(running, channelId, from, now);
        long totalLeft = recording.getTotalMinutes() * 60L
                - repo.sumRecordingSeconds(from, to) - runningSeconds(running, null, from, now);
        long left = Math.min(channelLeft, totalLeft);
        return new RecordingLeft(Math.max(0, left), left > 0 ? null : (channelLeft <= 0 ? "CHANNEL" : "TOTAL"));
    }

    /** Phần đang chạy của các buổi đang ghi (số giây của chúng chỉ chốt khi dừng); channelId null là mọi kênh. */
    private static long runningSeconds(List<LiveStream> running, UUID channelId, LocalDateTime monthStart,
                                       LocalDateTime now) {
        long total = 0;
        for (LiveStream s : running) {
            if (s.getRecordingStartedAt() == null || (channelId != null && !channelId.equals(s.getChannelId()))) {
                continue;
            }
            LocalDateTime since = s.getRecordingStartedAt().isBefore(monthStart) ? monthStart : s.getRecordingStartedAt();
            total += Math.max(0, Duration.between(since, now).getSeconds());
        }
        return total;
    }

    private String exhaustedMessage(RecordingLeft left) {
        return "CHANNEL".equals(left.limitedBy())
                ? "Kênh đã dùng hết " + recording.getChannelMinutes()
                        + " phút ghi hình của tháng này. Hãy tắt ghi hình hoặc thử lại vào tháng sau."
                : "WeTube đã dùng hết hạn mức ghi hình của tháng này. Hãy tắt ghi hình hoặc thử lại vào tháng sau.";
    }

    /** Lên sóng thì bắt đầu ghi (trình duyệt của chủ kênh ghi). Hết hạn mức chỉ làm buổi này không có bản ghi, buổi vẫn phát. */
    private void startRecording(LiveStream stream, LocalDateTime now) {
        RecordingLeft left = recordingLeft(stream.getChannelId(), now);
        if (!recording.isEnabled()) {
            skipRecording(stream, RecordingStatus.SKIPPED, "Ghi hình chưa được bật trên máy chủ nên buổi này không có video xem lại.");
        } else if (left.seconds() <= 0) {
            skipRecording(stream, RecordingStatus.SKIPPED, exhaustedMessage(left) + " Buổi vẫn phát nhưng không có video xem lại.");
        } else {
            stream.setRecordingStatus(RecordingStatus.RECORDING);
            stream.setRecordingStartedAt(now);
            stream.setRecordingSeconds(0);
            stream.setRecordingMessage(null);
        }
    }

    private void skipRecording(LiveStream stream, RecordingStatus status, String message) {
        stream.setRecordingStatus(status);
        stream.setRecordingMessage(truncate(message));
    }

    /** Job 15 giây, buổi đang ghi: chạm trần thì chốt phần đã ghi (trình duyệt cũng tự dừng ở mức này). */
    private void refreshRecording(LiveStream stream, LocalDateTime now) {
        if (recStatus(stream) == RecordingStatus.RECORDING && recordingLeft(stream.getChannelId(), now).seconds() <= 0) {
            stopRecording(stream, now,
                    "Đã ghi đủ phần ghi hình của tháng. Buổi vẫn đang phát nhưng không ghi tiếp.");
        }
    }

    /** Chuyển STOPPING và ước tính số giây theo đồng hồ; số thật có khi trình duyệt tải bản ghi lên. */
    private void stopRecording(LiveStream stream, LocalDateTime now, String message) {
        stream.setRecordingStatus(RecordingStatus.STOPPING);
        stream.setRecordingStoppedAt(now);
        if (stream.getRecordingStartedAt() != null) {
            stream.setRecordingSeconds((int) Math.max(0, Duration.between(stream.getRecordingStartedAt(), now).getSeconds()));
        }
        stream.setRecordingMessage(message == null ? null : truncate(message));
        repo.save(stream);
    }

    /** Quá {@link #UPLOAD_WAIT} mà trình duyệt chưa tải bản ghi lên (tab đã đóng...) thì thôi, không tính vào hạn mức. */
    private void giveUpWaitingForUpload(LiveStream stream, LocalDateTime now) {
        LocalDateTime stopped = stream.getRecordingStoppedAt() != null ? stream.getRecordingStoppedAt() : now;
        if (stopped.plus(UPLOAD_WAIT).isBefore(now)) {
            skipRecording(stream, RecordingStatus.FAILED,
                    "Không nhận được bản ghi từ trình duyệt của chủ kênh nên buổi này không có video xem lại.");
            stream.setRecordingSeconds(0);
            repo.save(stream);
        }
    }

    /**
     * Trình duyệt của chủ kênh tải bản ghi lên sau khi buổi kết thúc: lên Cloudinary rồi thành video công khai của kênh.
     * Tải lên chạy ngoài transaction (có thể mất cả phút); Cloudinary lỗi thì buổi vẫn chờ để chủ kênh Thử lại.
     * {@code reportedSeconds}: thời lượng trình duyệt đo, chỉ dùng khi Cloudinary không trả thời lượng.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public LiveView uploadReplay(UUID userId, UUID id, MultipartFile file, Double reportedSeconds) {
        tx.executeWithoutResult(status -> requireAwaitingUpload(owned(userId, id)));
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bản ghi rỗng");
        }
        String type = file.getContentType();
        if (type == null || !type.startsWith("video/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bản ghi phải là tệp video");
        }
        rateLimiter.check(Limit.LIVE_REPLAY, userId);
        if (!uploading.add(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bản ghi của buổi này đang được tải lên");
        }
        try {
            UploadedVideo uploaded;
            try {
                uploaded = cloudinary.uploadLiveRecording(file);
            } catch (RuntimeException e) {
                log.warn("[Live] Không tải được bản ghi của buổi {} lên Cloudinary: {}", id, e.getMessage());
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Không tải được bản ghi lên, hãy thử lại");
            }
            return tx.execute(status -> publishReplay(find(id), uploaded, reportedSeconds));
        } finally {
            uploading.remove(id);
        }
    }

    private static void requireAwaitingUpload(LiveStream stream) {
        if (recStatus(stream) == RecordingStatus.DONE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Buổi phát đã có video xem lại");
        }
        if (!isFinished(stream) || recStatus(stream) != RecordingStatus.STOPPING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Buổi phát này không chờ bản ghi");
        }
    }

    /** Bản ghi đã nằm trên Cloudinary: tạo video công khai của kênh, không thì xoá tệp và ghi lý do. */
    private LiveView publishReplay(LiveStream stream, UploadedVideo uploaded, Double reportedSeconds) {
        try {
            requireAwaitingUpload(stream); // job có thể đã bỏ chờ trong lúc tải lên
        } catch (ResponseStatusException e) {
            cloudinary.deleteVideo(uploaded.url());
            throw e;
        }
        int seconds = replaySeconds(stream, uploaded.duration(), reportedSeconds);
        stream.setRecordingSeconds(seconds);
        if (seconds < MIN_REPLAY_SECONDS) {
            skipRecording(stream, RecordingStatus.SKIPPED, "Bản ghi dưới " + MIN_REPLAY_SECONDS
                    + " giây nên không đăng thành video.");
            cloudinary.deleteVideo(uploaded.url());
        } else {
            try {
                stream.setReplayVideoId(videos.createLiveReplay(new LiveReplay(stream.getOwnerId(), stream.getTitle(),
                        replayDescription(stream), stream.getTopicId(), uploaded.url(), stream.getThumbnailUrl(),
                        seconds)));
                stream.setRecordingUrl(uploaded.url());
                stream.setRecordingStatus(RecordingStatus.DONE);
                // Giữ recordingMessage nếu có (vd. bản ghi bị cắt vì chạm trần) để chủ kênh biết
            } catch (RuntimeException e) {
                log.warn("[Live] Không tạo được video xem lại cho buổi {}: {}", stream.getId(), e.getMessage());
                String reason = e instanceof ResponseStatusException rse && rse.getReason() != null
                        ? rse.getReason() : "vui lòng thử lại sau";
                skipRecording(stream, RecordingStatus.FAILED, "Không đăng được bản ghi thành video: " + reason);
                cloudinary.deleteVideo(uploaded.url());
            }
        }
        repo.save(stream);
        return view(stream, channelOf(stream.getChannelId()), true);
    }

    /**
     * Thời lượng Cloudinary đo, không có thì số trình duyệt báo; không vượt số giây đã ghi theo đồng hồ của máy chủ, để
     * hạn mức không tính theo số trình duyệt tự gửi.
     */
    private static int replaySeconds(LiveStream stream, Float measured, Double reported) {
        double seconds = measured != null && measured > 0 ? measured
                : reported != null && reported > 0 ? reported : stream.getRecordingSeconds();
        if (stream.getRecordingSeconds() > 0) {
            seconds = Math.min(seconds, stream.getRecordingSeconds());
        }
        return (int) Math.round(seconds);
    }

    private static String replayDescription(LiveStream stream) {
        LocalDateTime when = stream.getStartedAt() != null ? stream.getStartedAt() : stream.getCreatedAt();
        String date = REPLAY_DATE.format(when.atZone(ZoneOffset.UTC).withZoneSameInstant(VIETNAM));
        String head = "Đã phát trực tiếp ngày " + date;
        String body = stream.getDescription();
        return body == null || body.isBlank() ? head : head + "\n\n" + body;
    }

    private static String truncate(String message) {
        return message.length() <= MESSAGE_MAX ? message : message.substring(0, MESSAGE_MAX - 1) + "…";
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
        return base.toBuilder().poll(poll).polls(history).build();
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
                    c == null ? null : c.getBackgroundImgUrl(), s.getThumbnailUrl(),
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
                .thumbnailUrl(s.getThumbnailUrl())
                .status(s.getStatus())
                .source(s.getSource())
                .protocol(s.getProtocol())
                .chatEnabled(s.isChatEnabled())
                .recordingEnabled(s.isRecordingEnabled())
                .recordingStatus(recStatus(s))
                .recordingMessage(includeIngress ? s.getRecordingMessage() : null)
                .replayVideoId(s.getReplayVideoId())
                .currentViewers(s.getCurrentViewers())
                .peakViewers(s.getPeakViewers())
                .createdAt(s.getCreatedAt())
                .startedAt(s.getStartedAt())
                .endedAt(s.getEndedAt())
                .ingress(hasIngress ? new IngressView(s.getIngressUrl(), s.getIngressKey()) : null)
                .build();
    }

    /** Xoá ảnh trên Cloudinary sau khi DB lưu xong, để lỗi lưu không làm mất ảnh đang dùng. */
    private void deleteImageAfterCommit(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cloudinary.deleteImage(url);
                }
            });
        } else {
            cloudinary.deleteImage(url);
        }
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
