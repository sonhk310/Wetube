package com.wetube.wetube_service.service.premium.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wetube.wetube_service.dto.PremiumUserDto;
import com.wetube.wetube_service.mapper.premium.PremiumUserMapper;
import com.wetube.wetube_service.repository.premium.PremiumUserRepository;
import com.wetube.wetube_service.service.premium.PremiumUserService;

import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PremiumUserServiceImpl implements PremiumUserService  {
    private final PremiumUserRepository premiumUserRepository;
    private final PremiumUserMapper premiumUserMapper;

    @Override
    public List<PremiumUserDto> getAllPremiumUsers() {
        return premiumUserRepository.findAll()
                .stream()
                .map(premiumUserMapper::toDto)
                .collect(Collectors.toList());
    }
}
