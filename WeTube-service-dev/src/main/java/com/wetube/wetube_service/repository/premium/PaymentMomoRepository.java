package com.wetube.wetube_service.repository.premium;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import com.wetube.wetube_service.entity.premium.PaymentMomo;

public interface PaymentMomoRepository extends CrudRepository<PaymentMomo, UUID> {
    Optional<PaymentMomo> findByOrderId(String orderId); 
} 
    

