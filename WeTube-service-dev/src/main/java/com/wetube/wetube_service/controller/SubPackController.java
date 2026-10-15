package com.wetube.wetube_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.wetube.wetube_service.dto.SubPacksDto;
import com.wetube.wetube_service.dto.request.SubPackRequestDto;
import com.wetube.wetube_service.service.premium.SubPackService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/subpacks")
@RequiredArgsConstructor
public class SubPackController {
    private final SubPackService subPackService;

    @GetMapping
    public ResponseEntity<List<SubPacksDto>> getAllSubPacks() {
        List<SubPacksDto> list = subPackService.getAllSubPacks();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubPacksDto> getSubPackById(@PathVariable UUID id) {
        SubPacksDto subPack = subPackService.getSubPackById(id);
        return ResponseEntity.ok(subPack);
    }

    @PostMapping
    public ResponseEntity<SubPacksDto> createSubPack(@RequestBody SubPackRequestDto request) {
        SubPacksDto created = subPackService.createSubPack(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubPacksDto> updateSubPack(
            @PathVariable UUID id,
            @RequestBody SubPackRequestDto request) {
        SubPacksDto updated = subPackService.updateSubPack(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubPack(@PathVariable UUID id) {
        subPackService.deleteSubPack(id);
        return ResponseEntity.noContent().build();
    }
}
