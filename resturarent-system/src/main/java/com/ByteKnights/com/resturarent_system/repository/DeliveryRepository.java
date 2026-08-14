package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.Delivery;
import com.ByteKnights.com.resturarent_system.entity.Order;
import com.ByteKnights.com.resturarent_system.entity.DeliveryStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    /**
     * Batch query: returns the set of order IDs (from the given list) that already have
     * a delivery record assigned to them.
     * <p>
     * Replaces a per-order loop of findByOrder() calls, eliminating the N+1 query pattern
     * in ManagerDriverServiceImpl.getDriverSummary() when filtering dispatchable orders.
     * Before: 1 query per completed order. After: 1 query total.
     */
    @Query("SELECT d.order.id FROM Delivery d WHERE d.order.id IN :orderIds")
    Set<Long> findOrderIdsAlreadyAssigned(@Param("orderIds") Collection<Long> orderIds);

    /**
     * Batch query: returns all active deliveries for a given list of staff (rider) IDs in a single
     * JOIN FETCH query, eagerly loading the associated Order to avoid lazy-load round trips.
     * <p>
     * Replaces a per-rider loop of findByDeliveryStaffIdAndDeliveryStatusIn() calls,
     * eliminating the N+1 query pattern in ManagerDriverServiceImpl.getDriverSummary()
     * when building the driver status list.
     * Before: 1 query per rider. After: 1 query total.
     */
    @Query("SELECT d FROM Delivery d JOIN FETCH d.order WHERE d.deliveryStaff.id IN :staffIds AND d.deliveryStatus IN :statuses")
    List<Delivery> findActiveDeliveriesForStaffBatch(
            @Param("staffIds") Collection<Long> staffIds,
            @Param("statuses") Collection<DeliveryStatus> statuses);

    /**
     * Paginated history query for a single driver.
     * Uses JOIN FETCH to eagerly load the associated Order in the same SQL query, preventing
     * N+1 lazy-load hits per row when mapping delivery history to DTOs.
     * Sorts by completedAt (COALESCE with assignedAt for cancelled orders) descending in SQL,
     * so no in-memory sort is needed in the service layer.
     * Accepts a Pageable to cap the result set (e.g., last 50 deliveries), preventing
     * unbounded memory growth for long-serving drivers.
     * <p>
     * Replaces the unbounded findByDeliveryStaffIdAndDeliveryStatusIn() call in
     * DeliveryOrderServiceImpl.getDeliveryHistory().
     */
    @Query("SELECT d FROM Delivery d JOIN FETCH d.order WHERE d.deliveryStaff.id = :staffId " +
           "AND d.deliveryStatus IN :statuses " +
           "ORDER BY COALESCE(d.deliveredAt, d.assignedAt) DESC")
    List<Delivery> findHistoryByStaffIdPaged(
            @Param("staffId") Long staffId,
            @Param("statuses") Collection<DeliveryStatus> statuses,
            Pageable pageable);
}
