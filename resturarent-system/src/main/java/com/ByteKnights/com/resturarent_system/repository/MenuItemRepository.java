package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.MenuItem;
import com.ByteKnights.com.resturarent_system.entity.MenuItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    
    // Fetches items by Branch ID, ensures they are APPROVED, and currently AVAILABLE
    List<MenuItem> findByBranchIdAndStatusAndIsAvailableTrue(Long branchId, MenuItemStatus status);

    Optional<MenuItem> findByBranchIdAndName(Long branchId, String name);

    void deleteByBranchIdAndNameIn(Long branchId, List<String> names);
}