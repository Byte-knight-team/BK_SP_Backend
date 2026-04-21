package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    boolean existsByBranchIdAndCategoryIdAndNameIgnoreCase(Long branchId, Long categoryId, String name);

    boolean existsByBranchIdAndCategoryIdAndNameIgnoreCaseAndIdNot(Long branchId, Long categoryId, String name, Long id);
}
