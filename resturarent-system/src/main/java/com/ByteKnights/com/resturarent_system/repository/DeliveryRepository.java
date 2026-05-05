package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.Delivery;
import com.ByteKnights.com.resturarent_system.entity.Order;
import com.ByteKnights.com.resturarent_system.entity.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.Collection;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    
    /**
     * Finds the delivery record associated with a specific master Order.
     * Useful when you have the Order entity and need to find its delivery details.
     */
    Optional<Delivery> findByOrder(Order order);
    
    /**
     * Finds all deliveries for a specific driver that match ANY of the provided statuses.
     * Typically used to find active orders (e.g., status is ACCEPTED or OUT_FOR_DELIVERY).
     */
    List<Delivery> findByDeliveryStaffIdAndDeliveryStatusIn(Long staffId, Collection<DeliveryStatus> statuses);

    /**
     * Finds all deliveries for a specific driver that match exactly ONE specific status.
     * Typically used to find new assignments (e.g., status is exactly ASSIGNED).
     */
    List<Delivery> findByDeliveryStaffIdAndDeliveryStatus(Long staffId, DeliveryStatus status);

    /**
     * Ensures that a specific order is actually assigned to a specific driver.
     * Returns an Optional that will be empty if the order belongs to someone else.
     */
    Optional<Delivery> findByOrderIdAndDeliveryStaffId(Long orderId, Long staffId);

    /**
     * Custom JPQL query used by the Manager module.
     * Retrieves all deliveries belonging to a specific restaurant branch that match certain statuses
     * (e.g., getting all active or pending deliveries for the Manager's dashboard).
     */
    @Query("SELECT d FROM Delivery d WHERE d.order.branch.id = :branchId AND d.deliveryStatus IN :statuses")
    List<Delivery> findByBranchIdAndStatusIn(
            @Param("branchId") Long branchId, 
            @Param("statuses") Collection<DeliveryStatus> statuses);

    /**
     * Custom JPQL query used by the Manager module for historical tracking.
     * Retrieves completed or cancelled deliveries for a specific branch.
     * Uses JOIN FETCH to instantly load the associated Order and Staff details in a single query
     * (preventing N+1 query performance issues), and sorts them by delivery time descending.
     */
    @Query("SELECT d FROM Delivery d JOIN FETCH d.order JOIN FETCH d.deliveryStaff WHERE d.order.branch.id = :branchId AND d.deliveryStatus IN :statuses ORDER BY d.deliveredAt DESC")
    List<Delivery> findDeliveryHistoryByBranchId(
            @Param("branchId") Long branchId,
            @Param("statuses") Collection<DeliveryStatus> statuses);
}
