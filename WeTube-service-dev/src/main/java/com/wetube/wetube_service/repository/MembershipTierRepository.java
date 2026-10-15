package com.wetube.wetube_service.repository;

import com.wetube.wetube_service.entity.channel.MembershipTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MembershipTierRepository extends JpaRepository<MembershipTier, UUID> {
}