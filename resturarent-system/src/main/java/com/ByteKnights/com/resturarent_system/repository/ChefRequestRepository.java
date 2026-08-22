package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.ChefRequest;
import com.ByteKnights.com.resturarent_system.entity.ChefRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChefRequestRepository extends JpaRepository<ChefRequest, Long> {

    // Find requests for a branch
    List<ChefRequest> findByBranchIdOrderByCreatedAtDesc(Long branchId);

    // Find only pending requests for a branch, ordered newest first
    List<ChefRequest> findByBranchIdAndStatusOrderByCreatedAtDesc(Long branchId, ChefRequestStatus status);
    // Find a specific chef's requests within a branch (newest first)
    List<ChefRequest> findByBranchIdAndChefNameOrderByCreatedAtDesc(Long branchId, String chefName);

    // Find only pending requests for a branch
    List<ChefRequest> findByBranchIdAndStatus(Long branchId, ChefRequestStatus status);

    // Count pending requests for a branch
    long countByBranchIdAndStatus(Long branchId, ChefRequestStatus status);

    // Paged + filtered "My Requests" list for a single chef — status and date optional. Mirrors
    // ReservationRepository.findFilteredByBranch's null-safe filter pattern.
    @Query(value = "SELECT r FROM ChefRequest r WHERE r.branch.id = :branchId AND r.chefName = :chefName " +
            "AND (:status IS NULL OR r.status = :status) " +
            "AND (:dayStart IS NULL OR (r.createdAt >= :dayStart AND r.createdAt < :dayEnd)) " +
            "ORDER BY r.createdAt DESC",
            countQuery = "SELECT COUNT(r) FROM ChefRequest r WHERE r.branch.id = :branchId AND r.chefName = :chefName " +
            "AND (:status IS NULL OR r.status = :status) " +
            "AND (:dayStart IS NULL OR (r.createdAt >= :dayStart AND r.createdAt < :dayEnd))")
    Page<ChefRequest> findMyRequestsFiltered(
            @Param("branchId") Long branchId,
            @Param("chefName") String chefName,
            @Param("status") ChefRequestStatus status,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd,
            Pageable pageable);

}
