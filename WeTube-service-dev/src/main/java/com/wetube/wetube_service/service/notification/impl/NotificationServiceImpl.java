package com.wetube.wetube_service.service.notification.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.notification.Notification;
import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.enumeration.SubscriptionType;
import com.wetube.wetube_service.repository.NotificationRepository;
import com.wetube.wetube_service.repository.UserRepository;
import com.wetube.wetube_service.repository.channel.SubscriptionRepository;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.security.VideoAccess;
import com.wetube.wetube_service.service.notification.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final int TEXT_MAX = 200;

    private final NotificationRepository notificationRepo;
    private final SubscriptionRepository subscriptionRepo;
    private final UserRepository userRepo;
    private final VideoRepository videoRepo;

    // REQUIRES_NEW: lỗi khi ghi thông báo không làm hỏng việc upload / bình luận đang chạy
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyNewVideo(UUID videoId, UUID ownerId, UUID channelId, boolean isShort) {
        if (channelId == null || notificationRepo.existsByTypeAndVideoId(Notification.Type.NEW_VIDEO, videoId)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<SubscriptionType> modes = isShort
                ? List.of(SubscriptionType.ALL)
                : List.of(SubscriptionType.ALL, SubscriptionType.PERSONALIZE);
        List<Notification> items = subscriptionRepo.findNotifiableSubscriberIds(channelId, modes).stream()
                .filter(id -> !id.equals(ownerId))
                .map(id -> Notification.builder()
                        .recipientId(id)
                        .type(Notification.Type.NEW_VIDEO)
                        .actorId(ownerId)
                        .videoId(videoId)
                        .createdAt(now)
                        .build())
                .toList();
        notificationRepo.saveAll(items);
        log.info("Thông báo video mới {} tới {} người đăng ký", videoId, items.size());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyComment(Notification.Type type, UUID recipientId, UUID actorId, UUID videoId, UUID commentId,
            String text) {
        if (recipientId == null || recipientId.equals(actorId)) {
            return;
        }
        String snippet = text == null ? null
                : text.length() > TEXT_MAX ? text.substring(0, TEXT_MAX - 1) + "…" : text;
        notificationRepo.save(Notification.builder()
                .recipientId(recipientId)
                .type(type)
                .actorId(actorId)
                .videoId(videoId)
                .commentId(commentId)
                .text(snippet)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyCommentHeart(UUID recipientId, UUID actorId, UUID videoId, UUID commentId, String text) {
        if (notificationRepo.existsByTypeAndCommentId(Notification.Type.COMMENT_HEART, commentId)) {
            return;
        }
        notifyComment(Notification.Type.COMMENT_HEART, recipientId, actorId, videoId, commentId, text);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationPage list(UUID userId, int page, int size) {
        Page<Notification> result = notificationRepo.findByRecipientIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        List<Notification> items = result.getContent();

        Map<UUID, AppUser> actors = userRepo.findAllById(
                        items.stream().map(Notification::getActorId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(AppUser::getId, Function.identity()));
        Map<UUID, Video> videos = videoRepo.findAllById(
                        items.stream().map(Notification::getVideoId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Video::getId, Function.identity()));

        List<NotificationView> views = items.stream()
                // Video đã bị xoá / ẩn / chuyển riêng tư thì bỏ thông báo đi
                .filter(n -> n.getVideoId() == null
                        || (videos.containsKey(n.getVideoId())
                            && VideoAccess.canView(videos.get(n.getVideoId()), userId, false)))
                .map(n -> {
                    AppUser actor = n.getActorId() != null ? actors.get(n.getActorId()) : null;
                    Video video = n.getVideoId() != null ? videos.get(n.getVideoId()) : null;
                    return new NotificationView(
                            n.getId(), n.getType(), n.isRead(), n.getCreatedAt(),
                            actor != null ? actor.getName() : null,
                            actor != null ? actor.getPicture() : null,
                            n.getVideoId(),
                            video != null ? video.getTitle() : null,
                            video != null ? video.getThumbnailUrl() : null,
                            n.getCommentId(),
                            n.getText());
                })
                .toList();
        return new NotificationPage(views, result.hasNext(), unreadCount(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notificationRepo.countByRecipientIdAndReadFalse(userId);
    }

    @Override
    @Transactional
    public void markRead(UUID userId, UUID notificationId) {
        notificationRepo.findByIdAndRecipientId(notificationId, userId).ifPresent(n -> {
            n.setRead(true);
            notificationRepo.save(n);
        });
    }

    @Override
    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepo.markAllRead(userId);
    }
}
