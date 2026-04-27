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
    Optional<Delivery> findByOrder(Order order);
    
    List<Delivery> findByDeliveryStaffIdAndDeliveryStatusIn(Long staffId, Collection<DeliveryStatus> statuses);

    @Query("SELECT d FROM Delivery d WHERE d.order.branch.id = :branchId AND d.deliveryStatus IN :statuses")
    List<Delivery> findByBranchIdAndStatusIn(
            @Param("branchId") Long branchId, 
            @Param("statuses") Collection<DeliveryStatus> statuses);
}
