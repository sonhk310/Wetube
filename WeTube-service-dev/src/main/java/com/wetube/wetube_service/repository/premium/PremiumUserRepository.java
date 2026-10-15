package com.wetube.wetube_service.repository.premium;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wetube.wetube_service.entity.premium.PremiumUser;

public interface PremiumUserRepository extends JpaRepository<PremiumUser, UUID> {

}
