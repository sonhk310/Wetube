package com.wetube.wetube_service.service.premium.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wetube.wetube_service.dto.PremiumUserDto;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.mapper.premium.PremiumUserMapper;
import com.wetube.wetube_service.repository.premium.PremiumUserRepository;
import com.wetube.wetube_service.service.premium.PremiumUserService;

import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PremiumUserServiceImpl implements PremiumUserService  {
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");

    private final PremiumUserRepository premiumUserRepository;
    private final PremiumUserMapper premiumUserMapper;
    private Clock clock = Clock.system(VIETNAM);

    @Override
    public List<PremiumUserDto> getAllPremiumUsers() {
        return premiumUserRepository.findAll()
                .stream()
                .map(premiumUserMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<LocalDate> premiumUntil(UUID userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(premiumUserRepository.findLatestEndDate(
                userId, ActiveStatus.ACTIVE, LocalDate.now(clock)));
    }

    @Override
    public Set<UUID> premiumUserIds(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(premiumUserRepository.findPremiumUserIds(userIds, ActiveStatus.ACTIVE, LocalDate.now(clock)));
    }
}
