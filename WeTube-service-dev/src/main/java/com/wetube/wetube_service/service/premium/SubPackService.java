package com.wetube.wetube_service.service.premium;

import java.util.List;
import java.util.UUID;

import com.wetube.wetube_service.dto.SubPacksDto;
import com.wetube.wetube_service.dto.request.SubPackRequestDto;

public interface SubPackService {
    List<SubPacksDto> getAllSubPacks();
    SubPacksDto getSubPackById(UUID id);
    SubPacksDto createSubPack(SubPackRequestDto request);
    SubPacksDto updateSubPack(UUID id, SubPackRequestDto request);
    void deleteSubPack(UUID id);
}
