package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.InventoryItem;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    // Find all inventory items for a specific branch
    List<InventoryItem> findByBranchId(Long branchId);

    // Find items where quantity is below reorder level (low stock)
    @Query("SELECT i FROM InventoryItem i WHERE i.branch.id = :branchId AND i.quantity <= i.reorderLevel")
    List<InventoryItem> findLowStockByBranchId(@Param("branchId") Long branchId);

    // Count low-stock items for a branch
    @Query("SELECT COUNT(i) FROM InventoryItem i WHERE i.branch.id = :branchId AND i.quantity <= i.reorderLevel")
    long countLowStockByBranchId(@Param("branchId") Long branchId);

    Optional<InventoryItem> findByName(String name);

}
