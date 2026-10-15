package com.wetube.wetube_service.repository.channel;

import com.wetube.wetube_service.entity.channel.CategoryVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryVideoRepository extends JpaRepository<CategoryVideo, UUID> {

    List<CategoryVideo> findByCategoryIdOrderByOrderPositionAsc(UUID categoryId);

    @Modifying
    @Query("UPDATE CategoryVideo cv SET cv.orderPosition = :orderPosition " +
            "WHERE cv.id = :id AND cv.category.id = :categoryId")
    void updateOrderPosition(@Param("id") UUID id,
                             @Param("categoryId") UUID categoryId,
                             @Param("orderPosition") Integer orderPosition);

    @Query("SELECT COUNT(cv) FROM CategoryVideo cv WHERE cv.category.channel.id = :channelId")
    Long countVideosByChannelId(@Param("channelId") UUID channelId);

    boolean existsByVideoIdAndCategoryChannelId(UUID videoId, UUID channelId);

    boolean existsByVideoIdAndCategoryId(UUID videoId, UUID categoryId);

    Optional<CategoryVideo> findByVideoIdAndCategoryId(UUID videoId, UUID categoryId);
}
