package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.PurchaseOrder;
import com.ByteKnights.com.resturarent_system.entity.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    // Find all POs for a branch ordered newest first
    List<PurchaseOrder> findByBranchIdOrderByCreatedAtDesc(Long branchId);

    // Find POs for a branch within a date range
    List<PurchaseOrder> findByBranchIdAndCreatedAtBetweenOrderByCreatedAtDesc(Long branchId, java.time.LocalDateTime start, java.time.LocalDateTime end);

    // Find POs for a branch filtered by status (for the status filter tabs on the frontend)
    List<PurchaseOrder> findByBranchIdAndStatusOrderByCreatedAtDesc(Long branchId, PurchaseOrderStatus status);

    // Count POs by branch and status — used in the procurement summary cards
    long countByBranchIdAndStatus(Long branchId, PurchaseOrderStatus status);

    // Count POs by vendor and status — used for vendor active PO count
    long countByVendorIdAndStatus(Long vendorId, PurchaseOrderStatus status);

    // Count POs that are still awaiting full delivery (SUBMITTED or PARTIALLY_RECEIVED)
    @Query("SELECT COUNT(p) FROM PurchaseOrder p WHERE p.branch.id = :branchId " +
           "AND p.status IN (com.ByteKnights.com.resturarent_system.entity.PurchaseOrderStatus.SUBMITTED, " +
           "com.ByteKnights.com.resturarent_system.entity.PurchaseOrderStatus.PARTIALLY_RECEIVED)")
    long countActivePendingByBranchId(@Param("branchId") Long branchId);

    // Get the highest existing PO sequence number for a given year (for auto-generating poNumber)
    @Query("SELECT MAX(p.poNumber) FROM PurchaseOrder p WHERE p.branch.id = :branchId AND p.poNumber LIKE CONCAT(:yearPrefix, '%')")
    String findMaxPoNumberByBranchAndYear(@Param("branchId") Long branchId, @Param("yearPrefix") String yearPrefix);
}
