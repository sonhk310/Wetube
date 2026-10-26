package com.wetube.wetube_service.service.watchRoom;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wetube.wetube_service.dto.room.RoomEvent;
import com.wetube.wetube_service.dto.room.RoomEvent.MemberView;
import com.wetube.wetube_service.dto.room.RoomIn;
import com.wetube.wetube_service.dto.room.VideoRoom;
import com.wetube.wetube_service.dto.room.WatchRoom;
import com.wetube.wetube_service.dto.room.WatchRoom.Chat;
import com.wetube.wetube_service.dto.room.WatchRoom.Member;
import com.wetube.wetube_service.dto.room.WatchRoom.Playback;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.repository.UserRepository;
import com.wetube.wetube_service.security.RateLimiter;
import com.wetube.wetube_service.security.RateLimiter.Limit;
import com.wetube.wetube_service.service.RedisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Phòng xem chung đồng bộ (PRD luồng 2). Server giữ trạng thái phát gốc của từng phòng; mọi thao tác
 * (Phát / Dừng / Tua / Đổi video) của bất kỳ thành viên nào đi qua đây, được đánh số phiên bản rồi phát lại cho cả phòng.
 *
 * Phòng nằm trong bộ nhớ (một tiến trình backend) và được ghi sang Redis sau mỗi thay đổi để sống qua lần deploy;
 * Redis không dùng được thì chạy như cũ, chỉ trong bộ nhớ. Mọi thay đổi của một phòng chạy trong synchronized(room).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WatchRoomService {

    public static final int MAX_MEMBERS = 20;
    private static final int MAX_QUEUE = 50;
    private static final int MAX_CHAT = 100;
    private static final int MAX_CHAT_LENGTH = 500;
    private static final double MAX_POSITION_SECONDS = 24 * 3600;

    private static final Duration REDIS_TTL = Duration.ofHours(6);
    /** Mất kết nối ngắn (tải lại trang, đổi mạng) không bị coi là rời phòng. */
    private static final long GRACE_MS = 15_000;
    private static final long EMPTY_ROOM_MS = 5 * 60_000;
    private static final long MAX_ROOM_LIFE_MS = 24 * 3600_000L;

    private static final Set<String> REACTIONS = Set.of("like", "love", "laugh", "wow", "sad", "fire");
    private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RedisService redis;
    private final RateLimiter rateLimiter;
    private final SimpMessagingTemplate messaging;
    private final UserRepository userRepo;
    private final RoomVideoLookup videos;

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final Map<String, WatchRoom> rooms = new ConcurrentHashMap<>();
    private final Map<String, SessionRef> sessions = new ConcurrentHashMap<>();

    private record SessionRef(String code, String userId) {
    }

    // ------------------------------------------------------------------ REST

    /** Tạo phòng; người tạo là chủ phòng. videoId / t (giây) tuỳ chọn: video đang xem và vị trí hiện tại. */
    public String create(UUID userId, String videoId, Double t) {
        if (!rateLimiter.tryAcquire(Limit.ROOM_CREATE, userId)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Bạn tạo phòng quá nhiều, vui lòng thử lại sau.");
        }
        VideoRoom video = null;
        if (videoId != null && !videoId.isBlank()) {
            video = videos.find(videoId);
            if (video == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy video");
            }
        }
        long now = System.currentTimeMillis();
        WatchRoom room = new WatchRoom();
        room.setHostUserId(userId.toString());
        room.setCreatedAt(now);
        room.setCurrentVideo(video);
        Playback state = room.getState();
        state.setVideoId(video != null ? video.getId() : null);
        state.setPlaying(video != null);
        state.setPosition(video != null ? sanitize(t == null ? 0 : t) : 0);
        state.setUpdatedAt(now);
        state.setVersion(1);

        String code;
        while (true) {
            code = generateCode();
            if (redis.get(redisKey(code)) != null) {
                continue;
            }
            room.setCode(code);
            if (rooms.putIfAbsent(code, room) == null) {
                break;
            }
        }
        synchronized (room) {
            persist(room);
        }
        log.debug("Room {} created by {}", code, userId);
        return code;
    }

    /** Thông tin công khai tối thiểu của phòng (để trang biết phòng còn không); null nếu không có. */
    public Map<String, Object> summary(String code) {
        WatchRoom room = find(code);
        if (room == null) {
            return null;
        }
        synchronized (room) {
            return Map.of(
                    "code", room.getCode(),
                    "memberCount", room.getMembers().size(),
                    "full", room.getMembers().size() >= MAX_MEMBERS);
        }
    }

    // ------------------------------------------------------------- STOMP

    public void join(String code, String sessionId, String userId) {
        WatchRoom room = find(code);
        if (room == null) {
            sendUser(userId, error("NOT_FOUND", "Phòng không tồn tại hoặc đã đóng"));
            return;
        }
        // Cùng một kết nối chuyển sang phòng khác: rời phòng cũ ngay
        SessionRef old = sessions.get(sessionId);
        if (old != null && !old.code().equals(room.getCode())) {
            leave(old.code(), sessionId, userId, true);
        }
        AppUser user = userRepo.findById(UUID.fromString(userId)).orElse(null);
        String name = user != null && user.getName() != null ? user.getName() : "Người dùng";
        String avatar = user != null ? user.getPicture() : null;

        synchronized (room) {
            if (room.getKicked().contains(userId)) {
                sendUser(userId, error("KICKED", "Bạn đã bị chủ phòng mời ra khỏi phòng này"));
                return;
            }
            Member member = room.getMembers().get(userId);
            if (member == null && room.getMembers().size() >= MAX_MEMBERS) {
                sendUser(userId, error("FULL", "Phòng đã đủ " + MAX_MEMBERS + " người"));
                return;
            }
            long now = System.currentTimeMillis();
            if (room.getMembers().values().stream().noneMatch(Member::isConnected)) {
                // Chưa ai kết nối: video không "chạy" trong lúc phòng trống, vào lại thì phát tiếp từ chỗ cũ
                room.getState().setUpdatedAt(now);
                room.getState().setVersion(room.getState().getVersion() + 1);
            }
            boolean isNew = member == null;
            if (isNew) {
                member = new Member();
                member.setUserId(userId);
                member.setJoinedAt(now);
                room.getMembers().put(userId, member);
            }
            member.setName(name);
            member.setAvatar(avatar);
            member.getSessions().add(sessionId);
            member.setDisconnectedAt(0);
            room.setEmptySince(0);
            sessions.put(sessionId, new SessionRef(room.getCode(), userId));

            sendUser(userId, RoomEvent.builder()
                    .type("SNAPSHOT")
                    .code(room.getCode())
                    .state(room.getState())
                    .video(room.getCurrentVideo())
                    .members(memberViews(room))
                    .hostUserId(room.getHostUserId())
                    .queue(room.getQueue())
                    .chat(room.getChat())
                    .serverTime(now)
                    .build());
            if (isNew) {
                broadcast(room, membersEvent(room));
                addSystem(room, name + " đã vào phòng");
            }
            persist(room);
        }
    }

    public void control(String code, String userId, RoomIn.Control msg) {
        WatchRoom room = find(code);
        if (room == null) {
            return;
        }
        if (!rateLimiter.tryAcquire(Limit.ROOM_CONTROL, userId)) {
            synchronized (room) {
                sendUser(userId, stateEvent(room, "STATE"));
            }
            return;
        }
        String type = msg.getType() == null ? "" : msg.getType();
        synchronized (room) {
            Member me = room.getMembers().get(userId);
            if (me == null) {
                return;
            }
            Playback state = room.getState();
            boolean ended = type.equals("ENDED");
            // Thao tác dựa trên trạng thái cũ bị bỏ (hai người bấm cùng lúc); người vừa thao tác được nối tiếp thao tác của chính họ
            boolean versionOk = msg.getVersion() == state.getVersion()
                    || (!ended && userId.equals(state.getLastActor()));
            if (!versionOk) {
                if (!ended) {
                    sendUser(userId, stateEvent(room, "STATE"));
                }
                return;
            }
            long now = System.currentTimeMillis();
            double position = sanitize(msg.getPosition());
            String text;
            boolean queueChanged = false;
            switch (type) {
                case "PLAY" -> {
                    state.setPlaying(true);
                    state.setPosition(position);
                    text = me.getName() + " đã phát video";
                }
                case "PAUSE" -> {
                    state.setPlaying(false);
                    state.setPosition(position);
                    text = me.getName() + " đã dừng video";
                }
                case "SEEK" -> {
                    state.setPosition(position);
                    text = me.getName() + " đã tua tới " + formatTime(position);
                }
                case "CHANGE" -> {
                    VideoRoom video = videos.find(msg.getVideoId());
                    if (video == null) {
                        sendUser(userId, error("INVALID", "Không phát được video này"));
                        return;
                    }
                    queueChanged = room.getQueue().removeIf(v -> v.getId().equals(video.getId()));
                    switchVideo(room, video);
                    text = me.getName() + " đã chuyển sang \"" + video.getTitle() + "\"";
                }
                case "ENDED" -> {
                    if (room.getQueue().isEmpty()) {
                        state.setPlaying(false);
                        state.setPosition(position);
                        text = null;
                    } else {
                        VideoRoom next = room.getQueue().remove(0);
                        queueChanged = true;
                        switchVideo(room, next);
                        text = "Đang phát video tiếp theo: \"" + next.getTitle() + "\"";
                    }
                }
                default -> {
                    return;
                }
            }
            state.setUpdatedAt(now);
            state.setVersion(state.getVersion() + 1);
            state.setLastActor(userId);
            broadcast(room, stateEvent(room, "STATE"));
            if (queueChanged) {
                broadcast(room, queueEvent(room));
            }
            if (text != null) {
                addSystem(room, text);
            }
            persist(room);
        }
    }

    public void chat(String code, String userId, RoomIn.Chat msg) {
        WatchRoom room = find(code);
        String content = msg.getContent() == null ? "" : msg.getContent().strip();
        if (room == null || content.isEmpty()) {
            return;
        }
        if (content.length() > MAX_CHAT_LENGTH) {
            content = content.substring(0, MAX_CHAT_LENGTH);
        }
        if (!rateLimiter.tryAcquire(Limit.ROOM_CHAT, userId)) {
            sendUser(userId, error("RATE", "Bạn nhắn quá nhanh, vui lòng chờ một chút"));
            return;
        }
        synchronized (room) {
            Member me = room.getMembers().get(userId);
            if (me == null) {
                return;
            }
            Chat chat = newChat("CHAT", content);
            chat.setUserId(userId);
            chat.setName(me.getName());
            chat.setAvatar(me.getAvatar());
            appendChat(room, chat);
            persist(room);
        }
    }

    public void reaction(String code, String userId, RoomIn.Reaction msg) {
        WatchRoom room = find(code);
        if (room == null || msg.getEmoji() == null || !REACTIONS.contains(msg.getEmoji())) {
            return;
        }
        if (!rateLimiter.tryAcquire(Limit.ROOM_REACTION, userId)) {
            return;
        }
        synchronized (room) {
            Member me = room.getMembers().get(userId);
            if (me == null) {
                return;
            }
            broadcast(room, RoomEvent.builder()
                    .type("REACTION")
                    .emoji(msg.getEmoji())
                    .userId(userId)
                    .name(me.getName())
                    .build());
        }
    }

    public void queue(String code, String userId, RoomIn.QueueOp msg) {
        WatchRoom room = find(code);
        if (room == null || msg.getOp() == null) {
            return;
        }
        if (!rateLimiter.tryAcquire(Limit.ROOM_CONTROL, userId)) {
            return;
        }
        VideoRoom video = msg.getOp().equals("ADD") ? videos.find(msg.getVideoId()) : null;
        synchronized (room) {
            Member me = room.getMembers().get(userId);
            if (me == null) {
                return;
            }
            switch (msg.getOp()) {
                case "ADD" -> {
                    if (video == null) {
                        sendUser(userId, error("INVALID", "Không thêm được video này"));
                        return;
                    }
                    if (room.getQueue().size() >= MAX_QUEUE) {
                        sendUser(userId, error("INVALID", "Hàng chờ đã đầy (" + MAX_QUEUE + " video)"));
                        return;
                    }
                    if (room.getQueue().stream().anyMatch(v -> v.getId().equals(video.getId()))) {
                        return;
                    }
                    if (room.getCurrentVideo() == null) {
                        // Phòng chưa có video: video đầu tiên được thêm phát luôn
                        long now = System.currentTimeMillis();
                        switchVideo(room, video);
                        Playback state = room.getState();
                        state.setUpdatedAt(now);
                        state.setVersion(state.getVersion() + 1);
                        state.setLastActor(userId);
                        broadcast(room, stateEvent(room, "STATE"));
                        addSystem(room, me.getName() + " đã chọn \"" + video.getTitle() + "\" để cả phòng cùng xem");
                        persist(room);
                        return;
                    }
                    room.getQueue().add(video);
                    broadcast(room, queueEvent(room));
                    addSystem(room, me.getName() + " đã thêm \"" + video.getTitle() + "\" vào hàng chờ");
                }
                case "REMOVE" -> {
                    if (!room.getQueue().removeIf(v -> v.getId().equals(msg.getVideoId()))) {
                        return;
                    }
                    broadcast(room, queueEvent(room));
                }
                default -> {
                    return;
                }
            }
            persist(room);
        }
    }

    /** Chủ phòng mời một người ra; người đó không vào lại được phòng này. */
    public void kick(String code, String userId, RoomIn.Kick msg) {
        WatchRoom room = find(code);
        if (room == null || msg.getUserId() == null) {
            return;
        }
        synchronized (room) {
            if (!userId.equals(room.getHostUserId()) || msg.getUserId().equals(userId)) {
                return;
            }
            Member target = room.getMembers().get(msg.getUserId());
            if (target == null) {
                return;
            }
            room.getKicked().add(target.getUserId());
            sendUser(target.getUserId(), RoomEvent.builder().type("KICKED").build());
            removeMember(room, target, target.getName() + " đã bị mời ra khỏi phòng");
            persist(room);
        }
    }

    /** Chủ phòng đóng phòng: mọi người nhận CLOSED, phòng bị xoá. */
    public void close(String code, String userId) {
        WatchRoom room = find(code);
        if (room == null) {
            return;
        }
        synchronized (room) {
            if (!userId.equals(room.getHostUserId())) {
                return;
            }
            deleteRoom(room, true);
        }
    }

    /** immediate: bấm "Rời phòng"; không thì là đóng / tải lại trang (chờ GRACE_MS phòng khi vào lại). */
    public void leave(String code, String sessionId, String userId, boolean immediate) {
        sessions.remove(sessionId);
        WatchRoom room = rooms.get(code);
        if (room == null) {
            return;
        }
        synchronized (room) {
            Member member = room.getMembers().get(userId);
            if (member == null) {
                return;
            }
            dropSession(room, member, sessionId, immediate);
        }
    }

    /** Trả giờ server để client tính độ lệch đồng hồ. */
    public void time(String userId, RoomIn.Time msg) {
        sendUser(userId, RoomEvent.builder()
                .type("TIME")
                .clientSent(msg.getClientSent())
                .serverTime(System.currentTimeMillis())
                .build());
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        SessionRef ref = sessions.remove(event.getSessionId());
        if (ref == null) {
            return;
        }
        WatchRoom room = rooms.get(ref.code());
        if (room == null) {
            return;
        }
        synchronized (room) {
            Member member = room.getMembers().get(ref.userId());
            if (member != null) {
                dropSession(room, member, event.getSessionId(), false);
            }
        }
    }

    // --------------------------------------------------------------- dọn dẹp

    @Scheduled(fixedDelay = 5000)
    public void sweep() {
        long now = System.currentTimeMillis();
        for (WatchRoom room : new ArrayList<>(rooms.values())) {
            synchronized (room) {
                if (now - room.getCreatedAt() > MAX_ROOM_LIFE_MS) {
                    deleteRoom(room, true);
                    continue;
                }
                boolean changed = false;
                for (Member member : new ArrayList<>(room.getMembers().values())) {
                    if (!member.isConnected() && member.getDisconnectedAt() > 0
                            && now - member.getDisconnectedAt() > GRACE_MS) {
                        removeMember(room, member, member.getName() + " đã rời phòng");
                        changed = true;
                    }
                }
                if (room.getMembers().isEmpty()) {
                    if (room.getEmptySince() == 0) {
                        room.setEmptySince(now);
                    } else if (now - room.getEmptySince() > EMPTY_ROOM_MS) {
                        deleteRoom(room, false);
                        continue;
                    }
                } else {
                    room.setEmptySince(0);
                    // Chủ phòng không còn trong phòng (vd. sau khi backend khởi động lại): người vào sớm nhất thay
                    if (!room.getMembers().containsKey(room.getHostUserId())) {
                        if (room.getHostAbsentSince() == 0) {
                            room.setHostAbsentSince(now);
                        } else if (now - room.getHostAbsentSince() > GRACE_MS) {
                            promoteHost(room);
                            changed = true;
                        }
                    } else {
                        room.setHostAbsentSince(0);
                    }
                }
                if (changed) {
                    persist(room);
                }
            }
        }
    }

    // ---------------------------------------------------------------- nội bộ

    /** Gọi trong synchronized(room). */
    private void dropSession(WatchRoom room, Member member, String sessionId, boolean immediate) {
        member.getSessions().remove(sessionId);
        if (member.isConnected()) {
            return;
        }
        long now = System.currentTimeMillis();
        member.setDisconnectedAt(now);
        if (room.getMembers().values().stream().noneMatch(Member::isConnected)) {
            // Không còn ai: giữ nguyên chỗ đang xem
            Playback state = room.getState();
            state.setPosition(state.positionAt(now));
            state.setUpdatedAt(now);
            state.setVersion(state.getVersion() + 1);
        }
        if (immediate) {
            removeMember(room, member, member.getName() + " đã rời phòng");
            persist(room);
        }
    }

    /** Gọi trong synchronized(room). */
    private void removeMember(WatchRoom room, Member member, String systemText) {
        room.getMembers().remove(member.getUserId());
        for (String sid : new ArrayList<>(member.getSessions())) {
            sessions.remove(sid);
        }
        if (member.getUserId().equals(room.getHostUserId()) && !room.getMembers().isEmpty()) {
            promoteHost(room);
        }
        if (room.getMembers().isEmpty()) {
            room.setEmptySince(System.currentTimeMillis());
        }
        broadcast(room, membersEvent(room));
        addSystem(room, systemText);
    }

    private void promoteHost(WatchRoom room) {
        room.getMembers().values().stream().findFirst().ifPresent(m -> room.setHostUserId(m.getUserId()));
        room.setHostAbsentSince(0);
        broadcast(room, membersEvent(room));
    }

    private void deleteRoom(WatchRoom room, boolean notify) {
        rooms.remove(room.getCode());
        redis.delete(redisKey(room.getCode()));
        for (Member member : room.getMembers().values()) {
            member.getSessions().forEach(sessions::remove);
        }
        if (notify) {
            broadcast(room, RoomEvent.builder().type("CLOSED").build());
        }
        log.debug("Room {} deleted", room.getCode());
    }

    private void switchVideo(WatchRoom room, VideoRoom video) {
        room.setCurrentVideo(video);
        Playback state = room.getState();
        state.setVideoId(video.getId());
        state.setPlaying(true);
        state.setPosition(0);
    }

    private WatchRoom find(String code) {
        if (code == null || !code.matches("[A-Za-z0-9-]{1,20}")) {
            return null;
        }
        WatchRoom room = rooms.get(code);
        if (room != null) {
            return room;
        }
        String json = redis.get(redisKey(code));
        if (json == null) {
            return null;
        }
        try {
            WatchRoom restored = mapper.readValue(json, WatchRoom.class);
            WatchRoom previous = rooms.putIfAbsent(code, restored);
            return previous != null ? previous : restored;
        } catch (JsonProcessingException e) {
            log.warn("Cannot restore room {} from Redis: {}", code, e.getMessage());
            return null;
        }
    }

    private void persist(WatchRoom room) {
        try {
            redis.set(redisKey(room.getCode()), mapper.writeValueAsString(room), REDIS_TTL);
        } catch (JsonProcessingException e) {
            log.warn("Cannot save room {} to Redis: {}", room.getCode(), e.getMessage());
        }
    }

    private void broadcast(WatchRoom room, RoomEvent event) {
        messaging.convertAndSend("/topic/rooms/" + room.getCode(), event);
    }

    private void sendUser(String userId, RoomEvent event) {
        messaging.convertAndSendToUser(userId, "/queue/rooms", event);
    }

    private RoomEvent stateEvent(WatchRoom room, String type) {
        return RoomEvent.builder()
                .type(type)
                .state(room.getState())
                .video(room.getCurrentVideo())
                .serverTime(System.currentTimeMillis())
                .build();
    }

    private RoomEvent queueEvent(WatchRoom room) {
        return RoomEvent.builder().type("QUEUE").queue(room.getQueue()).build();
    }

    private RoomEvent membersEvent(WatchRoom room) {
        return RoomEvent.builder()
                .type("MEMBERS")
                .members(memberViews(room))
                .hostUserId(room.getHostUserId())
                .build();
    }

    private List<MemberView> memberViews(WatchRoom room) {
        return room.getMembers().values().stream()
                .map(m -> new MemberView(m.getUserId(), m.getName(), m.getAvatar(), m.getUserId().equals(room.getHostUserId())))
                .toList();
    }

    private static RoomEvent error(String code, String text) {
        return RoomEvent.builder().type("ERROR").error(code).text(text).build();
    }

    private void addSystem(WatchRoom room, String text) {
        appendChat(room, newChat("SYSTEM", text));
    }

    private void appendChat(WatchRoom room, Chat chat) {
        room.getChat().add(chat);
        while (room.getChat().size() > MAX_CHAT) {
            room.getChat().remove(0);
        }
        broadcast(room, RoomEvent.builder().type("CHAT").message(chat).build());
    }

    private static Chat newChat(String type, String content) {
        Chat chat = new Chat();
        chat.setId(UUID.randomUUID().toString());
        chat.setType(type);
        chat.setContent(content);
        chat.setAt(System.currentTimeMillis());
        return chat;
    }

    private static double sanitize(double seconds) {
        if (!Double.isFinite(seconds) || seconds < 0) {
            return 0;
        }
        return Math.min(seconds, MAX_POSITION_SECONDS);
    }

    static String formatTime(double seconds) {
        long total = (long) seconds;
        long h = total / 3600;
        long m = (total % 3600) / 60;
        long s = total % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%d:%02d", m, s);
    }

    private static String redisKey(String code) {
        return "room:" + code;
    }

    private static String generateCode() {
        return randomChars(3) + "-" + randomChars(4) + "-" + randomChars(3);
    }

    private static String randomChars(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}
