package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.PurchaseOrderLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PurchaseOrderLogRepository extends JpaRepository<PurchaseOrderLog, Long> {

    @Query("SELECT l FROM PurchaseOrderLog l WHERE l.purchaseOrder.branch.id = :branchId ORDER BY l.createdAt DESC")
    List<PurchaseOrderLog> findByBranchIdOrderByCreatedAtDesc(@Param("branchId") Long branchId);
}
