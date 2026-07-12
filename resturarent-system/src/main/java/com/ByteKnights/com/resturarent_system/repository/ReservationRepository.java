package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.Reservation;
import com.ByteKnights.com.resturarent_system.entity.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    //------------------------receptionist query START---------------------

    // All active (PENDING) reservations for a branch within a date range (soonest first).
    @Query("SELECT r FROM Reservation r WHERE r.branch.id = :branchId " +
           "AND r.status = 'PAID' " +
           "AND r.reservationTime >= :start AND r.reservationTime < :end " +
           "ORDER BY r.reservationTime ASC")
    List<Reservation> findByBranchAndDate(
            @Param("branchId") Long branchId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // Active (PENDING) reservations that include the given table and overlap the slot [startTime, endTime).
    // Joins the reservation_tables link so a booking spanning several tables is matched by any of them.
    @Query("SELECT r FROM Reservation r JOIN r.tables t WHERE t.id = :tableId " +
           "AND r.status IN ('CONFIRMED', 'PAID') " +
           "AND r.reservationTime < :endTime AND r.endTime > :startTime")
    List<Reservation> findOverlappingReservations(
            @Param("tableId") Long tableId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    // Paged + filtered reservations for a branch (any status) — for the Reservations page.
    // All filters are optional: pass null to skip a filter. Paging comes from the Pageable.
    // The optional tableNumber filter uses an EXISTS subquery over the reservation's tables so the
    // outer query stays one-row-per-reservation (clean pagination, no DISTINCT needed).
    // Ordering ("what's happening next" first): upcoming reservations first, soonest at the top;
    // past reservations after, most-recent first. CURRENT_TIMESTAMP splits upcoming vs. past.
    @Query(value = "SELECT r FROM Reservation r WHERE r.branch.id = :branchId " +
           "AND (:tableNumber IS NULL OR EXISTS (SELECT t FROM Reservation r2 JOIN r2.tables t WHERE r2.id = r.id AND t.tableNumber = :tableNumber)) " +
           "AND (:status IS NULL OR r.status = :status) " +
           "AND (:dayStart IS NULL OR (r.reservationTime >= :dayStart AND r.reservationTime < :dayEnd)) " +
           "ORDER BY CASE WHEN r.reservationTime >= CURRENT_TIMESTAMP THEN 0 ELSE 1 END ASC, " +
           "CASE WHEN r.reservationTime >= CURRENT_TIMESTAMP THEN r.reservationTime END ASC, " +
           "r.reservationTime DESC",
           countQuery = "SELECT COUNT(r) FROM Reservation r WHERE r.branch.id = :branchId " +
           "AND (:tableNumber IS NULL OR EXISTS (SELECT t FROM Reservation r2 JOIN r2.tables t WHERE r2.id = r.id AND t.tableNumber = :tableNumber)) " +
           "AND (:status IS NULL OR r.status = :status) " +
           "AND (:dayStart IS NULL OR (r.reservationTime >= :dayStart AND r.reservationTime < :dayEnd))")
    Page<Reservation> findFilteredByBranch(
            @Param("branchId") Long branchId,
            @Param("tableNumber") Integer tableNumber,
            @Param("status") ReservationStatus status,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd,
            Pageable pageable);

    //------------------------receptionist query END---------------------

    //------------------------customer query START---------------------
    Page<Reservation> findByCustomerIdOrderByReservationTimeDesc(Long customerId, Pageable pageable);
    
    Page<Reservation> findByCustomerIdAndStatusInOrderByReservationTimeDesc(Long customerId, java.util.List<ReservationStatus> statuses, Pageable pageable);
    //------------------------customer query END---------------------
}
