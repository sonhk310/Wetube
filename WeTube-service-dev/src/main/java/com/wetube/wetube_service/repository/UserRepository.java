package com.wetube.wetube_service.repository;

import com.wetube.wetube_service.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    Optional<AppUser> findByIdWithRoles(@Param("id") UUID id);}