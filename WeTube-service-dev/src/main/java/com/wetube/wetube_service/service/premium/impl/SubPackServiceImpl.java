package com.wetube.wetube_service.service.premium.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wetube.wetube_service.dto.SubPacksDto;
import com.wetube.wetube_service.dto.request.SubPackRequestDto;
import com.wetube.wetube_service.entity.premium.SubPack;
import com.wetube.wetube_service.exception.ResourceNotFoundException;
import com.wetube.wetube_service.mapper.premium.SubPackMapper;
import com.wetube.wetube_service.repository.premium.SubPackRepository;
import com.wetube.wetube_service.service.premium.SubPackService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SubPackServiceImpl implements SubPackService {
    
    private final SubPackRepository subPackRepository;
    private final SubPackMapper subPackMapper;

    @Override
    public List<SubPacksDto> getAllSubPacks() {
        return ((List<SubPack>) subPackRepository.findAll())
                .stream()
                .map(subPackMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public SubPacksDto getSubPackById(UUID id) {
        SubPack subPack = subPackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("subPack", "id", id.toString()));
        return subPackMapper.toDto(subPack);
    }

    @Override
    public SubPacksDto createSubPack(SubPackRequestDto request) {
        SubPack subPack = subPackMapper.toEntity(request);
        SubPack saved = subPackRepository.save(subPack);
        return subPackMapper.toDto(saved);
    }

    @Override
    public SubPacksDto updateSubPack(UUID id, SubPackRequestDto request) {
        SubPack subPack = subPackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("subPack", "Id", id.toString()));
        
        subPackMapper.updateEntityFromDto(request, subPack);
        
        SubPack updated = subPackRepository.save(subPack);
        return subPackMapper.toDto(updated);
    }

    @Override
    public void deleteSubPack(UUID id) {
        subPackRepository.deleteById(id);
    }
}
