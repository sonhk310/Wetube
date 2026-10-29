package com.wetube.wetube_service.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wetube.wetube_service.dto.membership.ChannelTiersDto;
import com.wetube.wetube_service.dto.membership.OwnerMembersDto;
import com.wetube.wetube_service.security.CurrentUser;
import com.wetube.wetube_service.service.membership.ChannelMembershipService;

import lombok.RequiredArgsConstructor;

/** Hội viên kênh (M1): cấp để người xem chọn, và Studio của chủ kênh. Tạo / sửa / xoá cấp ở MemberTierController. */
@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
public class ChannelMembershipController {

    private final ChannelMembershipService membershipService;

    /** Công khai: các cấp trả phí đang nhận hội viên; đăng nhập rồi thì kèm hội viên hiện tại của người xem. */
    @GetMapping("/{channelId}/tiers")
    public ResponseEntity<ChannelTiersDto> tiers(@PathVariable UUID channelId, @AuthenticationPrincipal Jwt jwt) {
        UUID viewerId = jwt == null ? null : CurrentUser.id(jwt);
        return ResponseEntity.ok(membershipService.tiersOf(channelId, viewerId));
    }

    /** Chủ kênh: các cấp, hội viên còn hạn và doanh thu. */
    @GetMapping("/me/members")
    public ResponseEntity<OwnerMembersDto> myMembers(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(membershipService.ownerView(CurrentUser.id(jwt)));
    }
}
