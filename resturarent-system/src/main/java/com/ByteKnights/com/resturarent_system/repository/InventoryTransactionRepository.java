package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository for performing database operations on InventoryTransaction
 * entities.
 */
@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    /**
     * Finds all transactions for a specific inventory item, ordered by newest
     * first.
     */
    List<InventoryTransaction> findByInventoryItemIdOrderByCreatedAtDesc(Long itemId);

    /**
     * Finds all transactions for all items in a specific branch, ordered by newest first.
     */
    List<InventoryTransaction> findByInventoryItemBranchIdOrderByCreatedAtDesc(Long branchId);
}
