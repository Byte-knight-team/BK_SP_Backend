package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.Order;
import com.ByteKnights.com.resturarent_system.entity.Payment;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrder(Order order);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.order.branch.id = :branchId AND p.paymentMethod = :method AND p.paymentStatus = 'SUCCESS'")
    java.math.BigDecimal sumAmountByBranchIdAndPaymentMethod(
            @org.springframework.data.repository.query.Param("branchId") Long branchId,
            @org.springframework.data.repository.query.Param("method") com.ByteKnights.com.resturarent_system.entity.PaymentMethod method);
}
