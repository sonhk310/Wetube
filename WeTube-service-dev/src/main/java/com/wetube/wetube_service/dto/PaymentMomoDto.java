package com.wetube.wetube_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.premium.SubPack;
import com.wetube.wetube_service.enumeration.PaymentStatus;

import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentMomoDto {
    @Id
    private UUID id;
    private UUID orderId;
    private PaymentStatus status;

    @ManyToOne
    private AppUser user;

    @ManyToOne
    private SubPack subpack;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
