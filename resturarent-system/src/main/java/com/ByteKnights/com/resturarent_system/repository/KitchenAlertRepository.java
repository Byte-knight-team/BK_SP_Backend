package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.KitchenAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface KitchenAlertRepository extends JpaRepository<KitchenAlert, Long> {

    List<KitchenAlert> findByBranchIdAndIsResolvedFalseOrderByCreatedAtDesc(Long branchId);

    // For receptionist: today's alerts (all) + previous days that are still unresolved
    @Query("SELECT a FROM KitchenAlert a WHERE a.branch.id = :branchId " +
           "AND (a.createdAt >= :startOfToday OR a.isResolved = false) " +
           "ORDER BY a.createdAt DESC")
    List<KitchenAlert> findReceptionistAlerts(
            @Param("branchId") Long branchId,
            @Param("startOfToday") LocalDateTime startOfToday);
    /*
    no need to write a query like this
        SELECT * FROM kitchen_alerts
        WHERE branch_id = ?
        AND is_resolved = false
        ORDER BY created_at DESC;
     */
}
