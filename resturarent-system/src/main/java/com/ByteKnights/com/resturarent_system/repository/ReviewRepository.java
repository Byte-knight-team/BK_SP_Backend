package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.Review;
import org.springframework.data.domain.Pageable;
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

    // Fetch only the 3 most recent order-level reviews with comments for landing page
    @Query("""
            SELECT r
            FROM Review r
            WHERE r.orderItem IS NULL
              AND r.comment IS NOT NULL
              AND r.comment <> ''
            ORDER BY r.createdAt DESC
            """)
    List<Review> findRecentOrderReviews(Pageable pageable);
}
