package com.wetube.wetube_service.repository.channel;

import com.wetube.wetube_service.entity.channel.Subscription;
import com.wetube.wetube_service.entity.compositekey.SubscriptionId;
import com.wetube.wetube_service.enumeration.SubscriptionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, SubscriptionId> {

    /** Các kênh người dùng đã đăng ký (bảng tin Đăng ký). */
    @Query("SELECT DISTINCT s.id.tier.channel.id FROM Subscription s WHERE s.id.subscriber.id = :userId")
    List<UUID> findSubscribedChannelIds(@Param("userId") UUID userId);

    /**
     * Người đăng ký kênh có chế độ thông báo nằm trong {@code modes}. Đăng ký cũ chưa có chế độ tính là
     * PERSONALIZE (mặc định lúc đăng ký).
     */
    @Query("""
            SELECT DISTINCT s.id.subscriber.id FROM Subscription s
            WHERE s.id.tier.channel.id = :channelId
              AND COALESCE(s.notificationMode, com.wetube.wetube_service.enumeration.SubscriptionType.PERSONALIZE) IN :modes""")
    List<UUID> findNotifiableSubscriberIds(@Param("channelId") UUID channelId,
                                           @Param("modes") Collection<SubscriptionType> modes);
    List<Subscription> findById_Subscriber_Id(UUID subscriberId);

    List<Subscription> findById_Tier_Id(UUID tierId);

    int countById_Tier_Channel_Id(UUID channelId);

    /** Thời điểm đăng ký (UTC) của kênh trong khoảng [from, to): trang Số liệu phân tích. */
    @Query("SELECT s.createdAt FROM Subscription s WHERE s.id.tier.channel.id = :channelId AND s.createdAt >= :from AND s.createdAt < :to")
    List<LocalDateTime> createdAtBetween(@Param("channelId") UUID channelId, @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    Optional<Subscription> findFirstById_Subscriber_IdAndId_Tier_Channel_Id(UUID userId, UUID channelId);
}
