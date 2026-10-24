package com.wetube.wetube_service.repository;

import com.wetube.wetube_service.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByEmail(String email);

    @Query("""
        select u from AppUser u
        left join fetch u.userRoles ur
        left join fetch ur.role r
        where u.id = :id
    """)
    Optional<AppUser> findByIdWithRoles(@Param("id") UUID id);

    @Query("select coalesce(u.historyPaused, false) from AppUser u where u.id = :id")
    Optional<Boolean> findHistoryPaused(@Param("id") UUID id);

    @Transactional
    @Modifying
    @Query("update AppUser u set u.historyPaused = :paused where u.id = :id")
    int setHistoryPaused(@Param("id") UUID id, @Param("paused") boolean paused);
}