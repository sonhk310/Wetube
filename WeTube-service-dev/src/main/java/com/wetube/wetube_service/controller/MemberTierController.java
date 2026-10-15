package com.wetube.wetube_service.controller;

import com.wetube.wetube_service.dto.request.MemberTierRequest;
import com.wetube.wetube_service.mapper.channel.MemberTierMapper;
import com.wetube.wetube_service.service.channel.MemberTierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;
@RestController
@RequestMapping("/api/tiers")
@RequiredArgsConstructor
public class MemberTierController {
    private final MemberTierService memberTierService;
    private final MemberTierMapper mapper;

    @PostMapping("/{channelId}")
    public ResponseEntity<Void> createTier(
            @PathVariable UUID channelId,
            @RequestBody MemberTierRequest request) {

        UUID createdId = memberTierService.addTier(
                channelId,
                mapper.requestToDto(request)
        );

        URI location = URI.create("/api/tiers/" + createdId);
        return ResponseEntity.created(location).build();
    }

    @PutMapping("/{tierId}")
    public ResponseEntity<Void> updateTier(
            @PathVariable UUID tierId,
            @RequestBody MemberTierRequest request) {

        memberTierService.updateTier(tierId, mapper.requestToDto(request));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{tierId}")
    public ResponseEntity<Void> deleteTier(@PathVariable UUID tierId) {
        memberTierService.deleteTier(tierId);
        return ResponseEntity.noContent().build();
    }
}