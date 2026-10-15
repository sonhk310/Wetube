package com.wetube.wetube_service.repository.channel;

import com.wetube.wetube_service.entity.channel.ChannelCategory;
import com.wetube.wetube_service.enumeration.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChannelCategoryRepository extends JpaRepository<ChannelCategory, UUID> {
    List<ChannelCategory> findByChannelIdOrderByOrderPositionAsc(UUID channelId);

    @Query("SELECT DISTINCT c FROM ChannelCategory c LEFT JOIN FETCH c.categoryVideos cv LEFT JOIN FETCH cv.video v WHERE c.channel.id = :channelId AND c.isVisible = true ORDER BY c.orderPosition ASC, cv.orderPosition ASC")
    List<ChannelCategory> findByChannelIdAndIsVisibleTrueWithVideos(@Param("channelId") UUID channelId);

    @Query("SELECT c FROM ChannelCategory c LEFT JOIN FETCH c.categoryVideos cv " +
            "LEFT JOIN FETCH cv.video WHERE c.channel.id = :channelId " +
            "ORDER BY c.orderPosition ASC, cv.orderPosition ASC")
    List<ChannelCategory> findByChannelIdWithVideos(@Param("channelId") UUID channelId);

    @Modifying
    @Query("UPDATE ChannelCategory c SET c.orderPosition = :orderPosition " +
            "WHERE c.id = :id AND c.channel.id = :channelId")
    void updateOrderPosition(@Param("id") UUID id,
                             @Param("channelId") UUID channelId,
                             @Param("orderPosition") Integer orderPosition);

    boolean existsByIdAndChannelId(UUID id, UUID channelId);

    @Modifying
    @Query("UPDATE ChannelCategory c SET c.isVisible = :isVisible WHERE c.id = :id AND c.channel.id = :channelId")
    void updateVisibility(@Param("id") UUID id, @Param("channelId") UUID channelId, @Param("isVisible") Boolean isVisible);

    Optional<ChannelCategory> findByChannelIdAndCategoryType(UUID channelId, CategoryType categoryType);
}