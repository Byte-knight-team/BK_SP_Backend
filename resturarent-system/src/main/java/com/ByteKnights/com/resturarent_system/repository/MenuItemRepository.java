package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.MenuItem;
import com.ByteKnights.com.resturarent_system.entity.MenuItemStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    boolean existsByBranchIdAndCategoryIdAndNameIgnoreCase(Long branchId, Long categoryId, String name);

    boolean existsByBranchIdAndCategoryIdAndNameIgnoreCaseAndIdNot(Long branchId, Long categoryId, String name, Long id);

    @EntityGraph(attributePaths = {"category", "branch"})
    List<MenuItem> findByStatus(MenuItemStatus status);

    @EntityGraph(attributePaths = {"category", "branch"})
    List<MenuItem> findByStatusAndCreatedByIn(MenuItemStatus status, List<Long> createdByUserIds);

    @EntityGraph(attributePaths = {"category", "branch"})
    List<MenuItem> findByStatusAndIsAvailableTrue(MenuItemStatus status);

    @EntityGraph(attributePaths = {"category", "branch"})
    List<MenuItem> findByBranchId(Long branchId);

    // Fetches items by Branch ID, ensures they are APPROVED, and currently AVAILABLE
    @EntityGraph(attributePaths = {"category", "branch"})
    List<MenuItem> findByBranchIdAndStatusAndIsAvailableTrue(Long branchId, MenuItemStatus status);

    @EntityGraph(attributePaths = {"category", "branch"})
    Optional<MenuItem> findByBranchIdAndName(Long branchId, String name);

    void deleteByBranchIdAndNameIn(Long branchId, List<String> names);

    @EntityGraph(attributePaths = {"category", "branch"})
    List<MenuItem> findByBranchIdAndStatus(Long targetBranchId, MenuItemStatus active);
    boolean existsByCategoryIdAndIsAvailableTrue(Long categoryId);

    boolean existsByCategoryId(Long categoryId);

        @Query("""
            SELECT DISTINCT mi.subCategory
            FROM MenuItem mi
            WHERE (:branchId IS NULL OR mi.branch.id = :branchId)
              AND (:categoryId IS NULL OR mi.category.id = :categoryId)
              AND mi.subCategory IS NOT NULL
            ORDER BY mi.subCategory
            """)
        List<String> findDistinctSubCategories(
            @Param("branchId") Long branchId,
            @Param("categoryId") Long categoryId);

    @Query("SELECT COUNT(DISTINCT mi.category.id) FROM MenuItem mi WHERE mi.branch.id = :branchId")
    long countDistinctCategoryByBranchId(@Param("branchId") Long branchId);

    long countByBranchId(Long branchId);

    long countByBranchIdAndIsAvailableTrue(Long branchId);

    long countByIsAvailableTrue();

    @Query("SELECT COUNT(DISTINCT mi.subCategory) FROM MenuItem mi WHERE mi.branch.id = :branchId AND mi.subCategory IS NOT NULL")
    long countDistinctSubCategoryByBranchId(@Param("branchId") Long branchId);

    @Query("SELECT COUNT(DISTINCT mi.subCategory) FROM MenuItem mi WHERE mi.subCategory IS NOT NULL")
    long countDistinctSubCategory();

    @Query("""
        SELECT m, 
               COALESCE(AVG(r.rating), 0.0), 
               COUNT(r.id)
        FROM MenuItem m
        LEFT JOIN Review r ON r.orderItem.menuItem.id = m.id
        WHERE m.branch.id = :branchId 
          AND m.status = :status 
          AND m.isAvailable = true
        GROUP BY m.id
    """)
    List<Object[]> findMenuItemsWithReviewStats(
        @Param("branchId") Long branchId, 
        @Param("status") MenuItemStatus status
    );
}
