package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.MenuItemUpdateRequest;
import com.ByteKnights.com.resturarent_system.entity.MenuItemUpdateRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MenuItemUpdateRequestRepository extends JpaRepository<MenuItemUpdateRequest, Long> {
    List<MenuItemUpdateRequest> findByStatus(MenuItemUpdateRequestStatus status);
    List<MenuItemUpdateRequest> findByChefId(Long chefId);

    // Paged + filtered "My Requests" list for a single chef — status and date optional. Mirrors
    // ReservationRepository.findFilteredByBranch's null-safe filter pattern.
    @Query(value = "SELECT r FROM MenuItemUpdateRequest r WHERE r.chef.id = :chefId " +
            "AND (:status IS NULL OR r.status = :status) " +
            "AND (:dayStart IS NULL OR (r.createdAt >= :dayStart AND r.createdAt < :dayEnd)) " +
            "ORDER BY r.createdAt DESC",
            countQuery = "SELECT COUNT(r) FROM MenuItemUpdateRequest r WHERE r.chef.id = :chefId " +
            "AND (:status IS NULL OR r.status = :status) " +
            "AND (:dayStart IS NULL OR (r.createdAt >= :dayStart AND r.createdAt < :dayEnd))")
    Page<MenuItemUpdateRequest> findMyRequestsFiltered(
            @Param("chefId") Long chefId,
            @Param("status") MenuItemUpdateRequestStatus status,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd,
            Pageable pageable);
}
