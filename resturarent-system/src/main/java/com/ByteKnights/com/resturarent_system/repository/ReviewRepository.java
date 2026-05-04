package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ByteKnights.com.resturarent_system.entity.Order;
import com.ByteKnights.com.resturarent_system.entity.OrderItem;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByOrderAndOrderItemIsNull(Order order);
    boolean existsByOrderItem(OrderItem orderItem);

    // Fetch the 3 most recent order-level reviews (not item reviews) for landing page
    @Query(value = "SELECT r FROM Review r WHERE r.orderItem IS NULL AND r.comment IS NOT NULL AND r.comment != '' ORDER BY r.createdAt DESC")
    List<Review> findRecentOrderReviews();
}
