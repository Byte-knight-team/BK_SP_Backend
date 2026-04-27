package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.MenuItem;
import com.ByteKnights.com.resturarent_system.entity.MenuItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    boolean existsByBranchIdAndCategoryIdAndNameIgnoreCase(Long branchId, Long categoryId, String name);

    boolean existsByBranchIdAndCategoryIdAndNameIgnoreCaseAndIdNot(Long branchId, Long categoryId, String name, Long id);

    List<MenuItem> findByStatus(MenuItemStatus status);

    List<MenuItem> findByStatusAndCreatedByIn(MenuItemStatus status, List<Long> createdByUserIds);

    List<MenuItem> findByStatusAndIsAvailableTrue(MenuItemStatus status);

    // Fetches items by Branch ID, ensures they are APPROVED, and currently AVAILABLE
    List<MenuItem> findByBranchIdAndStatusAndIsAvailableTrue(Long branchId, MenuItemStatus status);

    Optional<MenuItem> findByBranchIdAndName(Long branchId, String name);

    void deleteByBranchIdAndNameIn(Long branchId, List<String> names);

    List<MenuItem> findByBranchIdAndStatus(Long targetBranchId, MenuItemStatus active);
}
