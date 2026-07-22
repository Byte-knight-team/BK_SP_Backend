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

    /**
     * Finds all transactions for all items in a specific branch filtered by a list of transaction types, ordered by newest first.
     */
    List<InventoryTransaction> findByInventoryItemBranchIdAndTransactionTypeInOrderByCreatedAtDesc(
            Long branchId, List<com.ByteKnights.com.resturarent_system.entity.InventoryTransactionType> types);

    /**
     * Finds all transactions for all items in a specific branch within a date range, ordered by newest first.
     */
    List<InventoryTransaction> findByInventoryItemBranchIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long branchId, java.time.LocalDateTime start, java.time.LocalDateTime end);
}
