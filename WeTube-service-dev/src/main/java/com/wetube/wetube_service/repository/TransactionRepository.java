package com.wetube.wetube_service.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wetube.wetube_service.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    
}
