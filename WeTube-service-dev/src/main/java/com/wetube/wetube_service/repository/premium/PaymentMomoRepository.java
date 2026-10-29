package com.wetube.wetube_service.repository.premium;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.wetube.wetube_service.entity.premium.PaymentMomo;

public interface PaymentMomoRepository extends CrudRepository<PaymentMomo, UUID> {
    Optional<PaymentMomo> findByOrderId(String orderId);

    /** Như findByOrderId nhưng khoá dòng cho tới hết giao dịch: hai lần /return đồng thời không cấp gói hai lần. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentMomo p where p.orderId = :orderId")
    Optional<PaymentMomo> findByOrderIdForUpdate(@Param("orderId") String orderId);
} 
    

