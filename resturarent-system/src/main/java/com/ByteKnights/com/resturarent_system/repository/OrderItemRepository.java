package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderIdIn(List<Long> orderIds);

    // --- Kitchen Queries START ---

    // 1. kitchen dashboard top 5 most popular meals with count
    // Native Queries: Direct SQL execution on the database.
    // Essential for complex analytical tasks or database-specific functions (like TIMESTAMPDIFF, HOUR, SUM, NOW).
    @Query(value = "SELECT oi.item_name, SUM(oi.quantity) as mealCount " +
            "FROM order_items oi " +
            "JOIN orders o ON oi.order_id = o.id " +
            "WHERE o.branch_id = :branchId " + // Branch Filter
            "AND o.status = 'COMPLETED' " + // Only count completed orders
            "AND o.created_at >= NOW() - INTERVAL 7 DAY " +
            "GROUP BY oi.item_name " +
            "ORDER BY mealCount DESC " +
            "LIMIT 5", nativeQuery = true)
    List<Object[]> findTop5PopularMealsInLast7DaysByBranch(@Param("branchId") Long branchId); // Return type = Object array List

    @Query(value = "SELECT SUM(oi.quantity) FROM order_items oi " +
            "JOIN orders o ON oi.order_id = o.id " +
            "WHERE o.branch_id = :branchId " + // Branch Filter
            "AND o.status = 'COMPLETED' " + // Only count completed orders
            "AND o.created_at >= NOW() - INTERVAL 7 DAY", nativeQuery = true)
    Long getTotalItemsSoldInLast7DaysByBranch(@Param("branchId") Long branchId);


    // number of meals completed by a line chef today
    @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.assignedLineChef.id = :chefId " +
            "AND oi.status = 'READY' AND oi.cookingCompletedAt >= :startOfToday")
    long countMealsPreparedToday(@Param("chefId") Long chefId, @Param("startOfToday") LocalDateTime startOfToday);

    // number of active (in-progress) items currently assigned to a line chef
    @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.assignedLineChef.id = :lineChefId " +
            "AND oi.status IN ('PENDING', 'PREPARING')")
    long countActiveItemsByLineChef(@Param("lineChefId") Long lineChefId);



    // --- Kitchen Queries END ---

    // ───────────────────────── Customer Statistics Dashboard ─────────────────────────

    @Query(value = "SELECT oi.item_name, mi.image_url, SUM(oi.quantity) AS cnt " +
            "FROM order_items oi JOIN orders o ON oi.order_id = o.id " +
            "LEFT JOIN menu_items mi ON oi.menu_item_id = mi.id " +
            "WHERE o.customer_id = :cid AND o.status NOT IN ('CANCELLED','REJECTED') " +
            "GROUP BY oi.item_name, mi.image_url ORDER BY cnt DESC LIMIT 3",
            nativeQuery = true)
    List<Object[]> findTop3ItemsByCustomer(@Param("cid") Long customerId);

    @Query(value = "SELECT COALESCE(SUM(oi.quantity), 0) FROM order_items oi " +
            "JOIN orders o ON oi.order_id = o.id " +
            "WHERE o.customer_id = :cid AND o.status NOT IN ('CANCELLED','REJECTED')",
            nativeQuery = true)
    Long countTotalItemsByCustomer(@Param("cid") Long customerId);
}