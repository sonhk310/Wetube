package com.wetube.wetube_service.service.membership;

import java.util.UUID;

import com.wetube.wetube_service.dto.MembershipPurchaseResponse;

public interface TransactionService {
    MembershipPurchaseResponse purchaseMembership(UUID userId, UUID membershipId);
}
