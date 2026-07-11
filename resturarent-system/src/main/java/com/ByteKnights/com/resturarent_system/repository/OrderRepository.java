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

        long countByStatusAndCreatedAtAfter(OrderStatus status, LocalDateTime startOfToday);

        long countByStatusIn(Collection<OrderStatus> statuses);

        long countByBranchId(Long branchId);

        long countByBranchIdAndStatusIn(Long branchId, Collection<OrderStatus> statuses);

        long countByCreatedAtAfter(LocalDateTime startOfToday);

        long countByBranchIdAndCreatedAtAfter(Long branchId, LocalDateTime startOfToday);

        long countByStatusInAndCreatedAtAfter(Collection<OrderStatus> statuses, LocalDateTime startOfToday);

        long countByBranchIdAndStatusInAndCreatedAtAfter(Long branchId, Collection<OrderStatus> statuses, LocalDateTime startOfToday);

        @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.paymentStatus IN :paymentStatuses")
        BigDecimal sumFinalAmountByPaymentStatusIn(@Param("paymentStatuses") Collection<PaymentStatus> paymentStatuses);

        @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.branch.id = :branchId AND o.paymentStatus IN :paymentStatuses")
        BigDecimal sumFinalAmountByBranchIdAndPaymentStatusIn(
                        @Param("branchId") Long branchId,
                        @Param("paymentStatuses") Collection<PaymentStatus> paymentStatuses);

        @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.paymentStatus IN :paymentStatuses AND o.createdAt >= :startOfToday")
        BigDecimal sumFinalAmountByPaymentStatusInAndCreatedAtAfter(
                        @Param("paymentStatuses") Collection<PaymentStatus> paymentStatuses, 
                        @Param("startOfToday") LocalDateTime startOfToday);

        @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.branch.id = :branchId AND o.paymentStatus IN :paymentStatuses AND o.createdAt >= :startOfToday")
        BigDecimal sumFinalAmountByBranchIdAndPaymentStatusInAndCreatedAtAfter(
                        @Param("branchId") Long branchId,
                        @Param("paymentStatuses") Collection<PaymentStatus> paymentStatuses,
                        @Param("startOfToday") LocalDateTime startOfToday);

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

        // kitchen dashboard stats

        @Query(value = "SELECT AVG(TIMESTAMPDIFF(SECOND, cooking_started_at, cooking_completed_at)) / 60.0 " +
                        "FROM orders WHERE branch_id = :branchId " + // breach filter
                        "AND status = 'COMPLETED' " +
                        "AND created_at >= :startOfToday " +
                        "AND cooking_started_at IS NOT NULL AND cooking_completed_at IS NOT NULL", nativeQuery = true)
        Double getAveragePreparationTimeTodayByBranch(
                        @Param("branchId") Long branchId,
                        @Param("startOfToday") LocalDateTime startOfToday);

        // Peak hours graph data based on order approval time

        @Query(value = "SELECT HOUR(approved_at) as hr, COUNT(id) as count " +
                        "FROM orders " +
                        "WHERE branch_id = :branchId " +
                        "AND status NOT IN ('CANCELLED', 'REJECTED') " + // Exclude canceled and rejected orders(extra
                                                                         // safe)
                        "AND approved_at >= NOW() - INTERVAL 7 DAY " +
                        "GROUP BY hr", nativeQuery = true)
        List<Object[]> findOrderCountByHourByBranch(@Param("branchId") Long branchId);

        List<Order> findByBranchIdAndOrderTypeAndStatus(
                        Long branchId,
                        com.ByteKnights.com.resturarent_system.entity.OrderType orderType,
                        OrderStatus status);

        // Counts today's orders for this branch that have unpaid cash (paymentStatus = PENDING)
        // Used by receptionist dashboard "Cash Due" KPI card
        long countByBranchIdAndPaymentStatusAndOrderTypeInAndCreatedAtBetween(
                Long branchId,
                PaymentStatus paymentStatus,
                List<OrderType> orderTypes,
                LocalDateTime start,
                LocalDateTime end);


        @EntityGraph(attributePaths = "items")
        List<Order> findByBranchIdAndStatusAndOrderTypeIn(
                        Long branchId,
                        OrderStatus status,
                        List<OrderType> orderTypes);

        // Kitchen tab: PENDING/PREPARING orders, but exclude QR orders that already have a READY or SERVED item
        // (those are shown in the Ready tab instead)
        @EntityGraph(attributePaths = "items")
        @Query("SELECT o FROM Order o " +
               "WHERE o.branch.id = :branchId " +
               "AND o.status = :status " +
               "AND o.orderType IN :orderTypes " +
               "AND o.createdAt BETWEEN :start AND :end " +
               "AND (o.orderType <> com.ByteKnights.com.resturarent_system.entity.OrderType.QR " +
               "     OR NOT EXISTS (" +
               "         SELECT i FROM OrderItem i WHERE i.order = o " +
               "         AND i.status IN (com.ByteKnights.com.resturarent_system.entity.OrderItemStatus.READY, " +
               "                          com.ByteKnights.com.resturarent_system.entity.OrderItemStatus.SERVED)))")
        List<Order> findKitchenOrdersExcludingQRWithReadyItems(
                @Param("branchId") Long branchId,
                @Param("status") OrderStatus status,
                @Param("orderTypes") List<OrderType> orderTypes,
                @Param("start") LocalDateTime start,
                @Param("end") LocalDateTime end);

        // QR orders in PENDING/PREPARING that have at least one READY or SERVED item — for receptionist Ready tab.
        // Using SERVED too so the order stays visible after partial serving while remaining items are still cooking.
        @EntityGraph(attributePaths = "items")
        @Query("SELECT DISTINCT o FROM Order o JOIN o.items i " +
               "WHERE o.branch.id = :branchId " +
               "AND o.orderType = com.ByteKnights.com.resturarent_system.entity.OrderType.QR " +
               "AND o.status IN (com.ByteKnights.com.resturarent_system.entity.OrderStatus.PENDING, " +
               "                 com.ByteKnights.com.resturarent_system.entity.OrderStatus.PREPARING) " +
               "AND i.status IN (com.ByteKnights.com.resturarent_system.entity.OrderItemStatus.READY, " +
               "                 com.ByteKnights.com.resturarent_system.entity.OrderItemStatus.SERVED) " +
               "AND o.createdAt BETWEEN :start AND :end")
        List<Order> findQROrdersWithAnyReadyItem(
                @Param("branchId") Long branchId,
                @Param("start") LocalDateTime start,
                @Param("end") LocalDateTime end);

        @EntityGraph(attributePaths = "items")
        List<Order> findByBranchIdAndStatusAndOrderTypeInAndCreatedAtBetween(
                        Long branchId,
                        OrderStatus status,
                        List<OrderType> orderTypes,
                        java.time.LocalDateTime start,
                        java.time.LocalDateTime end);

        List<Order> findByStatus(OrderStatus status, Sort sort);

        List<Order> findByStatusAndStatusUpdatedAtAfter(OrderStatus status, LocalDateTime startOfToday, Sort sort);

        @Query(value = "SELECT AVG(TIMESTAMPDIFF(SECOND, cooking_started_at, cooking_completed_at)) / 60.0 " +
                        "FROM orders WHERE status = 'COMPLETED' AND cooking_started_at IS NOT NULL AND cooking_completed_at IS NOT NULL", nativeQuery = true)
        Double getAveragePreparationTime();

        // Peak hours graph data based on order approval time
        @Query(value = "SELECT HOUR(approved_at) as hr, COUNT(id) as count " +
                        "FROM orders " +
                        "WHERE approved_at >= NOW() - INTERVAL 7 DAY " +
                        "GROUP BY hr", nativeQuery = true)
        List<Object[]> findOrderCountByHour();

        @Query(value = "SELECT DATE(created_at) as day, SUM(final_amount) as revenue, COUNT(id) as orders " +
                        "FROM orders " +
                        "WHERE branch_id = :branchId AND payment_status = 'PAID' AND created_at BETWEEN :start AND :end "
                        +
                        "GROUP BY day ORDER BY day", nativeQuery = true)
        List<Object[]> findRevenueTrendByBranchAndDates(
                        @Param("branchId") Long branchId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        // ───────────────────────── Customer Statistics Dashboard
        // ─────────────────────────

        @Query("SELECT COALESCE(SUM(o.finalAmount), 0), COALESCE(SUM(o.discountAmount), 0) " +
                        "FROM Order o WHERE o.customer.id = :cid " +
                        "AND o.status NOT IN (com.ByteKnights.com.resturarent_system.entity.OrderStatus.CANCELLED, " +
                        "com.ByteKnights.com.resturarent_system.entity.OrderStatus.REJECTED)")
        List<Object[]> findLifetimeFinancials(@Param("cid") Long customerId);

        @Query(value = "SELECT DATE_FORMAT(created_at, '%Y-%m') AS m, SUM(final_amount) " +
                        "FROM orders WHERE customer_id = :cid AND status NOT IN ('CANCELLED','REJECTED') " +
                        "AND created_at >= DATE_SUB(NOW(), INTERVAL 6 MONTH) GROUP BY m ORDER BY m", nativeQuery = true)
        List<Object[]> findMonthlySpendingTrend(@Param("cid") Long customerId);

        @Query("SELECT o.orderType, COUNT(o) FROM Order o WHERE o.customer.id = :cid " +
                        "AND o.status NOT IN (com.ByteKnights.com.resturarent_system.entity.OrderStatus.CANCELLED, " +
                        "com.ByteKnights.com.resturarent_system.entity.OrderStatus.REJECTED) GROUP BY o.orderType")
        List<Object[]> findOrderTypeCounts(@Param("cid") Long customerId);

        @Query("""
                SELECT
                    b.id,
                    b.name,
                    b.status,
                    COALESCE(SUM(o.finalAmount), 0),
                    COUNT(o.id),
                    COALESCE(SUM(
                        CASE
                            WHEN o.createdAt >= :todayStart THEN o.finalAmount
                            ELSE 0
                        END
                    ), 0),
                    COALESCE(SUM(
                        CASE
                            WHEN o.createdAt >= :todayStart THEN 1
                            ELSE 0
                        END
                    ), 0)
                FROM Branch b
                LEFT JOIN Order o
                    ON o.branch = b
                    AND o.paymentStatus IN :paymentStatuses
                    AND o.createdAt BETWEEN :start AND :end
                GROUP BY b.id, b.name, b.status
                ORDER BY COALESCE(SUM(o.finalAmount), 0) DESC
                """)
        List<Object[]> findSuperAdminBranchRevenueSummary(
                @Param("paymentStatuses") Collection<PaymentStatus> paymentStatuses,
                @Param("start") LocalDateTime start,
                @Param("end") LocalDateTime end,
                @Param("todayStart") LocalDateTime todayStart
        );

        // ── Receptionist Dashboard ──────────────────────────────────────────────

        long countByBranchIdAndStatusAndOrderTypeAndCreatedAtBetween(
                Long branchId, OrderStatus status, OrderType orderType,
                LocalDateTime start, LocalDateTime end);

        long countByBranchIdAndPaymentStatusAndOrderTypeAndCreatedAtBetween(
                Long branchId, PaymentStatus paymentStatus, OrderType orderType,
                LocalDateTime start, LocalDateTime end);

        // QR orders in PENDING/PREPARING that have NO READY or SERVED items — for kitchen QR count
        @Query("SELECT COUNT(DISTINCT o) FROM Order o " +
               "WHERE o.branch.id = :branchId " +
               "AND o.orderType = com.ByteKnights.com.resturarent_system.entity.OrderType.QR " +
               "AND o.status = :status " +
               "AND o.createdAt BETWEEN :start AND :end " +
               "AND NOT EXISTS (" +
               "    SELECT i FROM OrderItem i WHERE i.order = o " +
               "    AND i.status IN (com.ByteKnights.com.resturarent_system.entity.OrderItemStatus.READY, " +
               "                     com.ByteKnights.com.resturarent_system.entity.OrderItemStatus.SERVED))")
        long countKitchenQROrdersWithoutReadyItems(
                @Param("branchId") Long branchId,
                @Param("status") OrderStatus status,
                @Param("start") LocalDateTime start,
                @Param("end") LocalDateTime end);

        // QR orders in PENDING/PREPARING with at least one READY or SERVED item — for ready QR count
        @Query("SELECT COUNT(DISTINCT o) FROM Order o JOIN o.items i " +
               "WHERE o.branch.id = :branchId " +
               "AND o.orderType = com.ByteKnights.com.resturarent_system.entity.OrderType.QR " +
               "AND o.status IN (com.ByteKnights.com.resturarent_system.entity.OrderStatus.PENDING, " +
               "                 com.ByteKnights.com.resturarent_system.entity.OrderStatus.PREPARING) " +
               "AND i.status IN (com.ByteKnights.com.resturarent_system.entity.OrderItemStatus.READY, " +
               "                 com.ByteKnights.com.resturarent_system.entity.OrderItemStatus.SERVED) " +
               "AND o.createdAt BETWEEN :start AND :end")
        long countQROrdersWithAnyReadyItem(
                @Param("branchId") Long branchId,
                @Param("start") LocalDateTime start,
                @Param("end") LocalDateTime end);

        // Completed orders by type in last 7 days (for pie chart)
        @Query(value = "SELECT order_type, COUNT(*) FROM orders " +
               "WHERE branch_id = :branchId AND status = 'SERVED' " +
               "AND created_at BETWEEN :start AND :end " +
               "GROUP BY order_type", nativeQuery = true)
        List<Object[]> countCompletedOrdersByType(
                @Param("branchId") Long branchId,
                @Param("start") LocalDateTime start,
                @Param("end") LocalDateTime end);
}
