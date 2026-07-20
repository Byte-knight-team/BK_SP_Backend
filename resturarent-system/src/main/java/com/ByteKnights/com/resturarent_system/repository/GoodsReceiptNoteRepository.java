package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.GoodsReceiptNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoodsReceiptNoteRepository extends JpaRepository<GoodsReceiptNote, Long> {

    // Find all GRNs for a branch ordered by most recent first — used in the GRN history table
    List<GoodsReceiptNote> findByPurchaseOrderBranchIdOrderByReceivedAtDesc(Long branchId);

    // Find all GRNs linked to a specific Purchase Order — used when checking PO completion status
    List<GoodsReceiptNote> findByPurchaseOrderId(Long purchaseOrderId);
}
