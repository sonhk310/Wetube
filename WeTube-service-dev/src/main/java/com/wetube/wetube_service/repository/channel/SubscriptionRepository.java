package com.wetube.wetube_service.repository.channel;

import com.wetube.wetube_service.entity.channel.Subscription;
import com.wetube.wetube_service.entity.compositekey.SubscriptionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, SubscriptionId> {
    List<Subscription> findById_Subscriber_Id(UUID subscriberId);

    List<Subscription> findById_Tier_Id(UUID tierId);

    int countById_Tier_Channel_Id(UUID channelId);

    boolean existsById_Subscriber_IdAndId_Tier_Channel_Id(UUID userId, UUID channelId);
}
