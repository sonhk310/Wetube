package com.wetube.wetube_service.repository;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.wetube.wetube_service.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    boolean existsByMembership_Id(UUID tierId);

    @Query("select coalesce(sum(t.amount), 0) from Transaction t where t.channel.id = :channelId")
    BigDecimal totalAmount(@Param("channelId") UUID channelId);

    @Query("select coalesce(sum(t.wetubeFee), 0) from Transaction t where t.channel.id = :channelId")
    BigDecimal totalFee(@Param("channelId") UUID channelId);

    @Query("select coalesce(sum(t.channelRevenue), 0) from Transaction t where t.channel.id = :channelId")
    BigDecimal totalChannelRevenue(@Param("channelId") UUID channelId);
}
