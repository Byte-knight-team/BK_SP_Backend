package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.Order;
import com.ByteKnights.com.resturarent_system.entity.OrderStatus;
import com.ByteKnights.com.resturarent_system.entity.PaymentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    long countByStatus(OrderStatus status);

    long countByBranchIdAndStatus(Long branchId, OrderStatus status);

    long countByStatusIn(Collection<OrderStatus> statuses);

    long countByBranchId(Long branchId);

    long countByBranchIdAndStatusIn(Long branchId, Collection<OrderStatus> statuses);

    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.paymentStatus IN :paymentStatuses")
    BigDecimal sumFinalAmountByPaymentStatusIn(@Param("paymentStatuses") Collection<PaymentStatus> paymentStatuses);

    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.branch.id = :branchId AND o.paymentStatus IN :paymentStatuses")
    BigDecimal sumFinalAmountByBranchIdAndPaymentStatusIn(
            @Param("branchId") Long branchId,
            @Param("paymentStatuses") Collection<PaymentStatus> paymentStatuses);

        List<Order> findByPaymentStatusInAndCreatedAtBetween(
            Collection<PaymentStatus> paymentStatuses,
            LocalDateTime start,
            LocalDateTime end);

        List<Order> findByBranchIdAndPaymentStatusInAndCreatedAtBetween(
            Long branchId,
            Collection<PaymentStatus> paymentStatuses,
            LocalDateTime start,
            LocalDateTime end);

    @EntityGraph(attributePaths = "items")
    List<Order> findTop5ByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "items")
    List<Order> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "items")
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    @EntityGraph(attributePaths = "items")
    Optional<Order> findOrderById(Long id);

    List<Order> findByStatus(OrderStatus status, Sort sort);

    List<Order> findByStatusAndStatusUpdatedAtAfter(OrderStatus status, LocalDateTime startOfToday, Sort sort);


    // --- Kitchen Queries START ---

    // 1.kitchen dashboard stats

    @Query(value = "SELECT AVG(TIMESTAMPDIFF(SECOND, cooking_started_at, cooking_completed_at)) / 60.0 " +
            "FROM orders WHERE status = 'COMPLETED' AND cooking_started_at IS NOT NULL AND cooking_completed_at IS NOT NULL",
            nativeQuery = true)
    Double getAveragePreparationTime();

    // 2.Peak hours graph data based on order approval time
    @Query(value = "SELECT HOUR(approved_at) as hr, COUNT(id) as count " +
            "FROM orders " +
            "WHERE approved_at >= NOW() - INTERVAL 7 DAY " +
            "GROUP BY hr", nativeQuery = true)
    List<Object[]> findOrderCountByHour();

    // --- Kitchen Queries END ---
}
