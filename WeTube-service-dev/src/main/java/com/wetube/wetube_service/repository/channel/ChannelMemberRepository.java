package com.wetube.wetube_service.repository.channel;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.wetube.wetube_service.entity.channel.ChannelMember;

public interface ChannelMemberRepository extends JpaRepository<ChannelMember, UUID> {

    /** Các dòng còn hạn của một người ở một kênh, hạn xa nhất trước. */
    @Query("select m from ChannelMember m join fetch m.tier "
            + "where m.user.id = :userId and m.channel.id = :channelId and m.endDate >= :today "
            + "order by m.endDate desc")
    List<ChannelMember> findActive(@Param("userId") UUID userId, @Param("channelId") UUID channelId,
            @Param("today") LocalDate today);

    /** Các dòng còn hạn của nhiều người ở một kênh (huy hiệu ở bình luận / chat). */
    @Query("select m from ChannelMember m join fetch m.tier "
            + "where m.channel.id = :channelId and m.user.id in :userIds and m.endDate >= :today")
    List<ChannelMember> findActiveOf(@Param("channelId") UUID channelId, @Param("userIds") Collection<UUID> userIds,
            @Param("today") LocalDate today);

    /** Mọi dòng còn hạn của kênh (Studio: danh sách hội viên). */
    @Query("select m from ChannelMember m join fetch m.tier join fetch m.user "
            + "where m.channel.id = :channelId and m.endDate >= :today order by m.endDate desc")
    List<ChannelMember> findActiveByChannel(@Param("channelId") UUID channelId, @Param("today") LocalDate today);

    boolean existsByTier_Id(UUID tierId);
}
