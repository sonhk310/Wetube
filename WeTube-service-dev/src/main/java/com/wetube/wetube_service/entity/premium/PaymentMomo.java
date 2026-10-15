package com.wetube.wetube_service.entity.premium;

import java.time.LocalDateTime;
import java.util.UUID;

import com.wetube.wetube_service.entity.AppUser;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import com.wetube.wetube_service.enumeration.PaymentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "payment_momo")
public class PaymentMomo {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;
    
   private Long amount;

    @Column(name = "event_type")
    private String eventType;

    private String orderId;

    @Column(columnDefinition = "json")
    private String payload;

    @ManyToOne
    @JoinColumn(name = "premium_user_id")
    private PremiumUser premiumUser;

    private String provider;

    private String requestId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentStatus status;

    @ManyToOne
    @JoinColumn(name = "sub_package_id")
    private SubPack subpack;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private AppUser user;

    @CreationTimestamp
    private LocalDateTime createAt;

    @UpdateTimestamp
    private LocalDateTime updateAt; 
}
