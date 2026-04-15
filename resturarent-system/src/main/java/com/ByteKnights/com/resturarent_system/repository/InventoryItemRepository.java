package com.byteknights.com.resturarent_system.repository;

import com.byteknights.com.resturarent_system.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    // TODO: Add custom query methods as needed
}
