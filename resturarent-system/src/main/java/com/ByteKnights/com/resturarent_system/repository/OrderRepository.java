package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.Order;
import com.ByteKnights.com.resturarent_system.entity.OrderStatus;
import com.ByteKnights.com.resturarent_system.entity.PaymentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    long countByStatus(OrderStatus status);

    long countByStatusIn(Collection<OrderStatus> statuses);

    long countByBranchId(Long branchId);

    long countByBranchIdAndStatusIn(Long branchId, Collection<OrderStatus> statuses);

    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.paymentStatus IN :paymentStatuses")
    BigDecimal sumFinalAmountByPaymentStatusIn(@Param("paymentStatuses") Collection<PaymentStatus> paymentStatuses);

    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.branch.id = :branchId AND o.paymentStatus IN :paymentStatuses")
    BigDecimal sumFinalAmountByBranchIdAndPaymentStatusIn(
            @Param("branchId") Long branchId,
            @Param("paymentStatuses") Collection<PaymentStatus> paymentStatuses);

    @EntityGraph(attributePaths = "items")
    List<Order> findTop5ByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "items")
    List<Order> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "items")
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    @EntityGraph(attributePaths = "items")
    Optional<Order> findOrderById(Long id);
}
