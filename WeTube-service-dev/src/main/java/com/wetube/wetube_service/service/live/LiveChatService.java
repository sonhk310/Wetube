package com.wetube.wetube_service.service.live;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wetube.wetube_service.dto.live.LiveChatMessage;
import com.wetube.wetube_service.dto.live.LiveEvent;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.live.LiveStream;
import com.wetube.wetube_service.enumeration.LiveStatus;
import com.wetube.wetube_service.repository.UserRepository;
import com.wetube.wetube_service.repository.live.LiveStreamRepository;
import com.wetube.wetube_service.security.RateLimiter;
import com.wetube.wetube_service.security.RateLimiter.Limit;
import com.wetube.wetube_service.service.RedisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Chat của buổi phát trực tiếp. Chạy trong bộ nhớ và được ghi sang Redis sau mỗi thay đổi để sống qua lần deploy;
 * Redis không dùng được thì chỉ còn bộ nhớ. Người gửi luôn là người đăng nhập của kết nối STOMP. Văn bản được lưu
 * nguyên văn: client hiển thị dạng text, không dựng HTML.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiveChatService implements LiveCleanup {

    static final int MAX_MESSAGES = 100;
    static final int MAX_LENGTH = 200;
    private static final Duration TTL = Duration.ofHours(13);

    private final LiveStreamRepository repo;
    private final UserRepository userRepo;
    private final RedisService redis;
    private final RateLimiter rateLimiter;
    private final LiveEventPublisher publisher;
    private final ObjectMapper mapper;

    private final Map<UUID, Deque<LiveChatMessage>> rooms = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public void post(UUID streamId, String userId, String raw) {
        String text = raw == null ? "" : raw.strip();
        if (text.isEmpty()) {
            return;
        }
        LiveStream stream = repo.findById(streamId).orElse(null);
        if (stream == null || stream.getStatus() != LiveStatus.LIVE) {
            error(userId, "Buổi phát chưa bắt đầu hoặc đã kết thúc");
            return;
        }
        if (!stream.isChatEnabled()) {
            error(userId, "Chat đã bị tắt");
            return;
        }
        if (text.length() > MAX_LENGTH) {
            error(userId, "Tin nhắn tối đa " + MAX_LENGTH + " ký tự");
            return;
        }
        if (!rateLimiter.tryAcquire(Limit.LIVE_CHAT, userId)) {
            error(userId, "Bạn nhắn quá nhanh, vui lòng chờ một chút");
            return;
        }
        AppUser sender = userRepo.findById(UUID.fromString(userId)).orElse(null);
        if (sender == null) {
            return;
        }
        LiveChatMessage message = new LiveChatMessage(
                UUID.randomUUID().toString(),
                userId,
                sender.getName(),
                sender.getPicture(),
                sender.getChannel() == null ? null : sender.getChannel().getHandle(),
                text,
                stream.getOwnerId().toString().equals(userId),
                System.currentTimeMillis());

        Deque<LiveChatMessage> messages = messagesOf(streamId);
        List<LiveChatMessage> snapshot;
        synchronized (messages) {
            messages.addLast(message);
            while (messages.size() > MAX_MESSAGES) {
                messages.removeFirst();
            }
            snapshot = List.copyOf(messages);
        }
        persist(streamId, snapshot);
        publisher.toStream(streamId, LiveEvent.builder().type("CHAT").message(message).build());
    }

    /** Chủ kênh xoá một tin; người khác nhận lỗi riêng. */
    public void delete(UUID streamId, String userId, String messageId) {
        LiveStream stream = repo.findById(streamId).orElse(null);
        if (stream == null) {
            return;
        }
        if (!stream.getOwnerId().toString().equals(userId)) {
            error(userId, "Chỉ chủ kênh mới xoá được tin nhắn");
            return;
        }
        Deque<LiveChatMessage> messages = messagesOf(streamId);
        List<LiveChatMessage> snapshot;
        synchronized (messages) {
            if (!messages.removeIf(m -> m.id().equals(messageId))) {
                return;
            }
            snapshot = List.copyOf(messages);
        }
        persist(streamId, snapshot);
        publisher.toStream(streamId, LiveEvent.builder().type("CHAT_DELETED").messageId(messageId).build());
    }

    /** Các tin gần nhất cho người mới vào; rỗng nếu chat tắt hoặc buổi không còn phát. */
    public List<LiveChatMessage> recent(UUID streamId) {
        LiveStream stream = repo.findById(streamId).orElse(null);
        if (stream == null || stream.getStatus() != LiveStatus.LIVE || !stream.isChatEnabled()) {
            return List.of();
        }
        Deque<LiveChatMessage> messages = messagesOf(streamId);
        synchronized (messages) {
            return List.copyOf(messages);
        }
    }

    @Override
    public void onEnded(UUID streamId) {
        rooms.remove(streamId);
        redis.delete(key(streamId));
    }

    private Deque<LiveChatMessage> messagesOf(UUID streamId) {
        return rooms.computeIfAbsent(streamId, this::restore);
    }

    /** Sau khi backend khởi động lại: đọc các tin đã lưu trong Redis (nếu có). */
    private Deque<LiveChatMessage> restore(UUID streamId) {
        Deque<LiveChatMessage> messages = new ArrayDeque<>();
        try {
            String json = redis.get(key(streamId));
            if (json != null) {
                messages.addAll(mapper.readValue(json, new TypeReference<List<LiveChatMessage>>() {
                }));
            }
        } catch (Exception e) {
            log.warn("[Live] Không khôi phục được chat của buổi {} từ Redis: {}", streamId, e.getMessage());
        }
        return messages;
    }

    private void persist(UUID streamId, List<LiveChatMessage> snapshot) {
        try {
            redis.set(key(streamId), mapper.writeValueAsString(snapshot), TTL);
        } catch (Exception e) {
            log.warn("[Live] Không lưu được chat của buổi {} vào Redis: {}", streamId, e.getMessage());
        }
    }

    private void error(String userId, String text) {
        publisher.toUser(userId, LiveEvent.builder().type("ERROR").text(text).build());
    }

    private static String key(UUID streamId) {
        return "live:chat:" + streamId;
    }
}
