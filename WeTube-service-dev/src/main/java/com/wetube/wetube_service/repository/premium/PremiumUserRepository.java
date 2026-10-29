package com.wetube.wetube_service.repository.premium;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.wetube.wetube_service.entity.premium.PremiumUser;
import com.wetube.wetube_service.enumeration.ActiveStatus;

public interface PremiumUserRepository extends JpaRepository<PremiumUser, UUID> {

    /** Trong số userIds, những người có gói còn hiệu lực từ {@code today} (một truy vấn cho cả danh sách). */
    @Query("select distinct p.user.id from PremiumUser p "
            + "where p.user.id in :userIds and p.status = :status and p.endDate >= :today")
    List<UUID> findPremiumUserIds(@Param("userIds") Collection<UUID> userIds, @Param("status") ActiveStatus status,
            @Param("today") LocalDate today);

    /** Ngày hết hạn xa nhất trong các gói của người dùng còn hiệu lực từ {@code today}; null nếu không có. */
    @Query("select max(p.endDate) from PremiumUser p "
            + "where p.user.id = :userId and p.status = :status and p.endDate >= :today")
    LocalDate findLatestEndDate(@Param("userId") UUID userId, @Param("status") ActiveStatus status,
            @Param("today") LocalDate today);
}
