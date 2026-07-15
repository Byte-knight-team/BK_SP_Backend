package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.LoyaltyTransaction;
import com.ByteKnights.com.resturarent_system.entity.LoyaltyTransactionType;
import com.ByteKnights.com.resturarent_system.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, Long> {
    List<LoyaltyTransaction> findByOrder(Order order);

    boolean existsByOrderAndTransactionType(Order order, LoyaltyTransactionType transactionType);

    // ───────────────────────── Customer Statistics Dashboard ─────────────────────────

    @Query("SELECT lt.transactionType, COALESCE(SUM(lt.points), 0) FROM LoyaltyTransaction lt " +
            "WHERE lt.customer.id = :cid AND lt.transactionType IN (" +
            "com.ByteKnights.com.resturarent_system.entity.LoyaltyTransactionType.EARN, " +
            "com.ByteKnights.com.resturarent_system.entity.LoyaltyTransactionType.REDEEM) " +
            "GROUP BY lt.transactionType")
    List<Object[]> findStatisticsPoints(@Param("cid") Long customerId);

    @Query("SELECT COALESCE(SUM(lt.points), 0) FROM LoyaltyTransaction lt " +
            "WHERE lt.customer.id = :cid AND lt.transactionType = com.ByteKnights.com.resturarent_system.entity.LoyaltyTransactionType.EARN")
    Integer sumPointsEarned(@Param("cid") Long customerId);

    @Query("SELECT COALESCE(SUM(lt.points), 0) FROM LoyaltyTransaction lt " +
            "WHERE lt.customer.id = :cid AND lt.transactionType = com.ByteKnights.com.resturarent_system.entity.LoyaltyTransactionType.REDEEM")
    Integer sumPointsRedeemed(@Param("cid") Long customerId);
}
