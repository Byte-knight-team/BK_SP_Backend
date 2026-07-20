package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.Order;
import com.ByteKnights.com.resturarent_system.entity.Payment;
import com.ByteKnights.com.resturarent_system.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrder(Order order);

    // Safe read: returns the latest payment for an order, never throws on stray duplicates
    Optional<Payment> findFirstByOrderOrderByIdDesc(Order order);
    
    @Query("SELECT p.order.id FROM Payment p WHERE p.transactionReference = :txnRef")
    Optional<Long> findOrderIdByTransactionReference(@Param("txnRef") String txnRef);
    
    @Modifying
    @Transactional
    @Query("UPDATE Payment p SET p.paymentStatus = :status WHERE p.transactionReference = :txnRef")
    int updatePaymentStatusByTransactionReference(@Param("txnRef") String txnRef, @Param("status") PaymentStatus status);

    @Modifying
    @Transactional
    @Query("UPDATE Order o SET o.paymentStatus = :status WHERE o.id IN (SELECT p.order.id FROM Payment p WHERE p.transactionReference = :txnRef)")
    int updateOrderPaymentStatusByTxnRef(@Param("txnRef") String txnRef, @Param("status") PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.order.branch.id = :branchId AND p.paymentMethod = :method AND p.paymentStatus = 'PAID'")
    BigDecimal sumAmountByBranchIdAndPaymentMethod(
            @Param("branchId") Long branchId,
            @Param("method") com.ByteKnights.com.resturarent_system.entity.PaymentMethod method);

    // Total cash collected today for a specific order type (QR or ONLINE_PICKUP)
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.order.branch.id = :branchId " +
           "AND p.order.orderType = :orderType " +
           "AND p.paymentMethod = com.ByteKnights.com.resturarent_system.entity.PaymentMethod.CASH " +
           "AND p.paymentStatus = com.ByteKnights.com.resturarent_system.entity.PaymentStatus.PAID " +
           "AND p.paidAt BETWEEN :start AND :end")
    BigDecimal sumCashCollectedByOrderType(
            @Param("branchId") Long branchId,
            @Param("orderType") com.ByteKnights.com.resturarent_system.entity.OrderType orderType,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // Daily cash revenue by order type for last 7 days (for line chart)
    @Query(value = "SELECT DATE(p.paid_at) as day, COALESCE(SUM(p.amount), 0) as revenue " +
                   "FROM payments p JOIN orders o ON p.order_id = o.id " +
                   "WHERE o.branch_id = :branchId AND o.order_type = :orderType " +
                   "AND p.payment_method = 'CASH' AND p.payment_status = 'PAID' " +
                   "AND p.paid_at BETWEEN :start AND :end " +
                   "GROUP BY DATE(p.paid_at) ORDER BY day", nativeQuery = true)
    List<Object[]> findDailyRevenueByOrderType(
            @Param("branchId") Long branchId,
            @Param("orderType") String orderType,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
