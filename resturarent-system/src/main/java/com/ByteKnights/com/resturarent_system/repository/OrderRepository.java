package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.Order;
import com.ByteKnights.com.resturarent_system.entity.OrderStatus;
import com.ByteKnights.com.resturarent_system.entity.OrderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
        List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

        @EntityGraph(attributePaths = "items")
        List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

        // retrive paginated orders without type filter
        @EntityGraph(attributePaths = "items")
        @Query(value = "SELECT o FROM Order o WHERE o.customer.id = :customerId " +
                        "AND (o.orderType IS NOT NULL) " +
                        "AND (:isActive IS NULL OR " +
                        "     (:isActive = true AND o.status IN ('PLACED', 'APPROVED', 'PENDING', 'PREPARING', 'READY', 'COMPLETED', 'OUT_FOR_DELIVERY', 'ARRIVED', 'ON_HOLD')) OR "
                        +
                        "     (:isActive = false AND o.status IN ('SERVED', 'CANCELLED', 'REJECTED'))" +
                        ") ORDER BY o.createdAt DESC", countQuery = "SELECT COUNT(o) FROM Order o WHERE o.customer.id = :customerId "
                                        +
                                        "AND (o.orderType IS NOT NULL) " +
                                        "AND (:isActive IS NULL OR " +
                                        "     (:isActive = true AND o.status IN ('PLACED', 'APPROVED', 'PENDING', 'PREPARING', 'READY', 'COMPLETED', 'OUT_FOR_DELIVERY', 'ARRIVED', 'ON_HOLD')) OR "
                                        +
                                        "     (:isActive = false AND o.status IN ('SERVED', 'CANCELLED', 'REJECTED'))" +
                                        ")")
        Page<Order> findFilteredOrdersWithoutType(@Param("customerId") Long customerId,
                        @Param("isActive") Boolean isActive,
                        Pageable pageable);

        // retrive paginated orders with a type filter
        @EntityGraph(attributePaths = "items")
        @Query(value = "SELECT o FROM Order o WHERE o.customer.id = :customerId " +
                        "AND (o.orderType = :type) " +
                        "AND (:isActive IS NULL OR " +
                        "     (:isActive = true AND o.status IN ('PLACED', 'APPROVED', 'PENDING', 'PREPARING', 'READY', 'COMPLETED', 'OUT_FOR_DELIVERY', 'ARRIVED', 'ON_HOLD')) OR "
                        +
                        "     (:isActive = false AND o.status IN ('SERVED', 'CANCELLED', 'REJECTED'))" +
                        ") ORDER BY o.createdAt DESC", countQuery = "SELECT COUNT(o) FROM Order o WHERE o.customer.id = :customerId "
                                        +
                                        "AND (o.orderType = :type) " +
                                        "AND (:isActive IS NULL OR " +
                                        "     (:isActive = true AND o.status IN ('PLACED', 'APPROVED', 'PENDING', 'PREPARING', 'READY', 'COMPLETED', 'OUT_FOR_DELIVERY', 'ARRIVED', 'ON_HOLD')) OR "
                                        +
                                        "     (:isActive = false AND o.status IN ('SERVED', 'CANCELLED', 'REJECTED'))" +
                                        ")")
        Page<Order> findFilteredOrders(@Param("customerId") Long customerId,
                        @Param("type") OrderType type,
                        @Param("isActive") Boolean isActive,
                        Pageable pageable);

        // retrieve orders with type filter
        @EntityGraph(attributePaths = "items")
        @Query("SELECT o FROM Order o WHERE o.customer.id = :customerId " +
                        "AND (o.orderType = :type) " +
                        "AND (:isActive IS NULL OR " +
                        "     (:isActive = true AND o.status IN ('PLACED', 'APPROVED', 'PENDING', 'PREPARING', 'READY', 'COMPLETED', 'OUT_FOR_DELIVERY', 'ARRIVED', 'ON_HOLD')) OR "
                        +
                        "     (:isActive = false AND o.status IN ('SERVED', 'CANCELLED', 'REJECTED'))" +
                        ") ORDER BY o.createdAt DESC")
        List<Order> findFilteredOrders(@Param("customerId") Long customerId,
                        @Param("type") OrderType type,
                        @Param("isActive") Boolean isActive);

        @EntityGraph(attributePaths = "items")
        Optional<Order> findByIdAndCustomerId(Long id, Long customerId);

        @EntityGraph(attributePaths = "items")
        Optional<Order> findOrderById(Long id);

        @EntityGraph(attributePaths = "items")
        List<Order> findTop5ByBranchIdOrderByCreatedAtDesc(Long branchId);

        @EntityGraph(attributePaths = "items")
        List<Order> findTop50ByBranchIdOrderByCreatedAtDesc(Long branchId);

        @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.branch.id = :branchId AND o.paymentStatus = 'PAID' AND o.createdAt BETWEEN :start AND :end")
        BigDecimal sumFinalAmountByBranchIdAndPaidStatusAndCreatedAtBetween(
                        @Param("branchId") Long branchId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        List<Order> findByBranchIdAndStatusAndStatusUpdatedAtAfter(Long branchId, OrderStatus status,
                        LocalDateTime startOfToday, Sort sort);

        long countByBranchIdAndStatusAndCreatedAtAfter(Long branchId, OrderStatus orderStatus,
                        LocalDateTime startOfToday);

        Optional<Order> findByIdAndBranchId(Long orderId, Long branchId);

        List<Order> findByTableIdAndStatusNotIn(Long id, List<OrderStatus> cancelled);

        @Query("SELECT COUNT(o) FROM Order o WHERE o.branch.id = :branchId AND o.paymentStatus = 'PAID' AND o.createdAt BETWEEN :start AND :end")
        long countByBranchIdAndPaidStatusAndCreatedAtBetween(
                        @Param("branchId") Long branchId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.branch.id = :branchId AND o.status IN :statuses")
        BigDecimal sumFinalAmountByBranchIdAndStatusIn(
                        @Param("branchId") Long branchId,
                        @Param("statuses") Collection<OrderStatus> statuses);

        @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.branch.id = :branchId AND o.orderType = :orderType AND o.paymentStatus IN :paymentStatuses")
        BigDecimal sumFinalAmountByBranchIdAndOrderTypeAndPaymentStatusIn(
                        @Param("branchId") Long branchId,
                        @Param("orderType") com.ByteKnights.com.resturarent_system.entity.OrderType orderType,
                        @Param("paymentStatuses") Collection<PaymentStatus> paymentStatuses);

        long countByBranchIdAndOrderTypeAndCreatedAtBetween(
                        Long branchId,
                        com.ByteKnights.com.resturarent_system.entity.OrderType orderType,
                        LocalDateTime start,
                        LocalDateTime end);

        // 1.kitchen dashboard stats

        @Query(value = "SELECT AVG(TIMESTAMPDIFF(SECOND, cooking_started_at, cooking_completed_at)) / 60.0 " +
                        "FROM orders WHERE branch_id = :branchId " + // breach filter
                        "AND status = 'COMPLETED' " +
                        "AND created_at >= :startOfToday " +
                        "AND cooking_started_at IS NOT NULL AND cooking_completed_at IS NOT NULL", nativeQuery = true)
        Double getAveragePreparationTimeTodayByBranch(
                        @Param("branchId") Long branchId,
                        @Param("startOfToday") LocalDateTime startOfToday);

        // 2.Peak hours graph data based on order approval time
        // Just adding the NOT IN line to your existing code!
        @Query(value = "SELECT HOUR(approved_at) as hr, COUNT(id) as count " +
                        "FROM orders " +
                        "WHERE branch_id = :branchId " +
                        "AND status NOT IN ('CANCELLED', 'REJECTED') " + // Exclude cancelled and rejected orders(extra
                                                                         // safe)
                        "AND approved_at >= NOW() - INTERVAL 7 DAY " +
                        "GROUP BY hr", nativeQuery = true)
        List<Object[]> findOrderCountByHourByBranch(@Param("branchId") Long branchId);

        List<Order> findByBranchIdAndOrderTypeAndStatus(
                        Long branchId,
                        com.ByteKnights.com.resturarent_system.entity.OrderType orderType,
                        OrderStatus status);

        List<Order> findByStatus(OrderStatus status, Sort sort);

        List<Order> findByStatusAndStatusUpdatedAtAfter(OrderStatus status, LocalDateTime startOfToday, Sort sort);

        // --- Kitchen Queries START ---

        // 1.kitchen dashboard stats

        @Query(value = "SELECT AVG(TIMESTAMPDIFF(SECOND, cooking_started_at, cooking_completed_at)) / 60.0 " +
                        "FROM orders WHERE status = 'COMPLETED' AND cooking_started_at IS NOT NULL AND cooking_completed_at IS NOT NULL", nativeQuery = true)
        Double getAveragePreparationTime();

        // 2.Peak hours graph data based on order approval time
        @Query(value = "SELECT HOUR(approved_at) as hr, COUNT(id) as count " +
                        "FROM orders " +
                        "WHERE approved_at >= NOW() - INTERVAL 7 DAY " +
                        "GROUP BY hr", nativeQuery = true)
        List<Object[]> findOrderCountByHour();

        // --- Kitchen Queries END ---

        @Query(value = "SELECT DATE(created_at) as day, SUM(final_amount) as revenue, COUNT(id) as orders " +
                        "FROM orders " +
                        "WHERE branch_id = :branchId AND payment_status = 'PAID' AND created_at BETWEEN :start AND :end "
                        +
                        "GROUP BY day ORDER BY day", nativeQuery = true)
        List<Object[]> findRevenueTrendByBranchAndDates(
                        @Param("branchId") Long branchId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);
}
