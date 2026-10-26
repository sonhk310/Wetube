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

    /**
     * Video vừa công khai lần đầu: báo cho người đăng ký kênh theo chế độ chuông của họ. Tất cả = mọi video kể cả
     * Shorts; Cá nhân hoá = chỉ video thường; Không có thông báo = không báo.
     */
    void notifyNewVideo(UUID videoId, UUID ownerId, UUID channelId, boolean isShort);

    /** Trả lời bình luận / bình luận video. Bỏ qua khi người nhận chính là người bình luận. */
    void notifyComment(Notification.Type type, UUID recipientId, UUID actorId, UUID videoId, UUID commentId, String text);

    /** Chủ video thả tim bình luận: báo người viết, mỗi bình luận một lần (bỏ tim rồi thả lại không báo nữa). */
    void notifyCommentHeart(UUID recipientId, UUID actorId, UUID videoId, UUID commentId, String text);

    NotificationPage list(UUID userId, int page, int size);

    long unreadCount(UUID userId);

    void markRead(UUID userId, UUID notificationId);

    void markAllRead(UUID userId);
}
