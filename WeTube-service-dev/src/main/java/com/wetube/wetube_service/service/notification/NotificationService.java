package com.wetube.wetube_service.service.notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.wetube.wetube_service.entity.notification.Notification;

public interface NotificationService {

    record NotificationView(
            UUID id,
            Notification.Type type,
            boolean read,
            LocalDateTime createdAt,
            String actorName,
            String actorPicture,
            UUID videoId,
            String videoTitle,
            String thumbnailUrl,
            UUID commentId,
            String text) {
    }

    record NotificationPage(List<NotificationView> items, boolean hasMore, long unread) {
    }

    /** Video vừa công khai lần đầu: báo cho người đăng ký kênh còn bật thông báo. */
    void notifyNewVideo(UUID videoId, UUID ownerId, UUID channelId);

    /** Trả lời bình luận / bình luận video. Bỏ qua khi người nhận chính là người bình luận. */
    void notifyComment(Notification.Type type, UUID recipientId, UUID actorId, UUID videoId, UUID commentId, String text);

    NotificationPage list(UUID userId, int page, int size);

    long unreadCount(UUID userId);

    void markRead(UUID userId, UUID notificationId);

    void markAllRead(UUID userId);
}
