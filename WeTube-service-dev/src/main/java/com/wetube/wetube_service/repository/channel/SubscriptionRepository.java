package com.wetube.wetube_service.repository.channel;

import com.wetube.wetube_service.entity.channel.Subscription;
import com.wetube.wetube_service.entity.compositekey.SubscriptionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, SubscriptionId> {

    /** Các kênh người dùng đã đăng ký (bảng tin Đăng ký). */
    @Query("SELECT DISTINCT s.id.tier.channel.id FROM Subscription s WHERE s.id.subscriber.id = :userId")
    List<UUID> findSubscribedChannelIds(@Param("userId") UUID userId);

    /** Người đăng ký kênh còn bật thông báo (khác NONE_NOTIFICATION). */
    @Query("""
            SELECT DISTINCT s.id.subscriber.id FROM Subscription s
            WHERE s.id.tier.channel.id = :channelId
              AND (s.notificationMode IS NULL
                   OR s.notificationMode <> com.wetube.wetube_service.enumeration.SubscriptionType.NONE_NOTIFICATION)""")
    List<UUID> findNotifiableSubscriberIds(@Param("channelId") UUID channelId);
    List<Subscription> findById_Subscriber_Id(UUID subscriberId);

    List<Subscription> findById_Tier_Id(UUID tierId);

    int countById_Tier_Channel_Id(UUID channelId);

    /** Thời điểm đăng ký (UTC) của kênh trong khoảng [from, to): trang Số liệu phân tích. */
    @Query("SELECT s.createdAt FROM Subscription s WHERE s.id.tier.channel.id = :channelId AND s.createdAt >= :from AND s.createdAt < :to")
    List<LocalDateTime> createdAtBetween(@Param("channelId") UUID channelId, @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    boolean existsById_Subscriber_IdAndId_Tier_Channel_Id(UUID userId, UUID channelId);
}
