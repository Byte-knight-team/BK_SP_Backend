package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // --- Kitchen Queries START ---

    //1. kitchen dashboard top 5 most popular meals with count
    @Query(value = "SELECT oi.item_name, SUM(oi.quantity) as mealCount " +
            "FROM order_items oi " +
            "JOIN orders o ON oi.order_id = o.id " +
            "WHERE o.created_at >= NOW() - INTERVAL 1 DAY " +
            "GROUP BY oi.item_name " +
            "ORDER BY mealCount DESC " +
            "LIMIT 5", nativeQuery = true)
    List<Object[]> findTop5PopularMeals(); // Return type = Object array List

    @Query(value = "SELECT SUM(oi.quantity) FROM order_items oi " +
            "JOIN orders o ON oi.order_id = o.id " +
            "WHERE o.created_at >= NOW() - INTERVAL 1 DAY", nativeQuery = true)
    Long getTotalItemsSoldInLast24Hours();


    // --- Kitchen Queries END ---
}
