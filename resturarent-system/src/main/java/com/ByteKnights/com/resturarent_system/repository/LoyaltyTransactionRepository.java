package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.LoyaltyTransaction;
import com.ByteKnights.com.resturarent_system.entity.LoyaltyTransactionType;
import com.ByteKnights.com.resturarent_system.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, Long> {
    List<LoyaltyTransaction> findByOrder(Order order);

    boolean existsByOrderAndTransactionType(Order order, LoyaltyTransactionType transactionType);
}
