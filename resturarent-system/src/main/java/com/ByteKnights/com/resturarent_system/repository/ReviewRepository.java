package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ByteKnights.com.resturarent_system.entity.Order;
import com.ByteKnights.com.resturarent_system.entity.OrderItem;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
  List<Review> findByOrderBranchIdAndCreatedAtBetween(Long branchId, java.time.LocalDateTime start, java.time.LocalDateTime end);

  boolean existsByOrderAndOrderItemIsNull(Order order);

  boolean existsByOrderItem(OrderItem orderItem);

  // Fetch only the 3 most recent order-level reviews with comments for landing
  // page
  @Query("""
      SELECT r
      FROM Review r
      WHERE r.orderItem IS NULL
        AND r.comment IS NOT NULL
        AND r.comment <> ''
        AND r.rating = 5
      ORDER BY r.createdAt DESC
      """)
  List<Review> findRecentOrderReviews(Pageable pageable);

  @Query("""
      SELECT AVG(r.rating)
      FROM Review r
      WHERE r.orderItem.menuItem.id = :menuItemId
      """)
  Double findAverageRatingByMenuItemId(Long menuItemId);

  @Query("""
      SELECT COUNT(r)
      FROM Review r
      WHERE r.orderItem.menuItem.id = :menuItemId
      """)
  Long countByMenuItemId(Long menuItemId);

  // Paginated item-level reviews for a specific menu item, newest first
  @Query("""
          SELECT r FROM Review r
          WHERE r.orderItem.menuItem.id = :menuItemId
            AND r.orderItem IS NOT NULL
          ORDER BY r.createdAt DESC
      """)
  Page<Review> findByMenuItemId(Long menuItemId, Pageable pageable);

  // Rating breakdown: count of reviews per star value (1-5) for the bar chart
  @Query("""
          SELECT r.rating, COUNT(r)
          FROM Review r
          WHERE r.orderItem.menuItem.id = :menuItemId
            AND r.orderItem IS NOT NULL
          GROUP BY r.rating
          ORDER BY r.rating DESC
      """)
  List<Object[]> countRatingsByMenuItemId(Long menuItemId);
}
