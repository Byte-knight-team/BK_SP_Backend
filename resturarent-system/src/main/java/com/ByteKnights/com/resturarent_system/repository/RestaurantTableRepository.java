package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

@Repository
/**
 * Persistence operations for restaurant tables.
 */
public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Long> {

    /**
     * Finds all tables for a branch.
     */
    List<RestaurantTable> findByBranchId(Long branchId);

    /**
     * Checks whether a table number is already used inside a branch.
     */
    boolean existsByBranchIdAndTableNumber(Long branchId, Integer tableNumber);

    /**
     * Checks table number uniqueness inside a branch excluding a given table id.
     */
    boolean existsByBranchIdAndTableNumberAndIdNot(Long branchId, Integer tableNumber, Long id);

    /**
     * Loads a table row with a write lock for concurrent-safe updates.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RestaurantTable t where t.id = :id")
    Optional<RestaurantTable> findByIdForUpdate(Long id);
}
