package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ByteKnights.com.resturarent_system.entity.Order;
import com.ByteKnights.com.resturarent_system.entity.OrderItem;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByOrderAndOrderItemIsNull(Order order);
    boolean existsByOrderItem(OrderItem orderItem);
}
