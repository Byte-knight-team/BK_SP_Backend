package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    //------------------------receptionist query START---------------------

    // Find an active (PENDING) reservation for a specific table
    @Query("SELECT r FROM Reservation r WHERE r.table.id = :tableId AND r.status = 'PENDING'")
    Optional<Reservation> findActiveReservationByTableId(Long tableId);

    // Find all active (PENDING) reservations for a branch within a date range
    @Query("SELECT r FROM Reservation r WHERE r.table.branch.id = :branchId " +
           "AND r.status = 'PENDING' " +
           "AND r.reservationTime >= :start AND r.reservationTime < :end " +
           "ORDER BY r.reservationTime ASC")
    List<Reservation> findByBranchAndDate(
            @Param("branchId") Long branchId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // Check if a table already has an active (PENDING) reservation overlapping the requested slot
    @Query("SELECT r FROM Reservation r WHERE r.table.id = :tableId " +
           "AND r.status = 'PENDING' " +
           "AND r.reservationTime < :endTime AND r.endTime > :startTime")
    List<Reservation> findOverlappingReservations(
            @Param("tableId") Long tableId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    // All reservations for a branch (any status) — newest reservation time first, for the Reservations page
    @Query("SELECT r FROM Reservation r WHERE r.table.branch.id = :branchId ORDER BY r.reservationTime DESC")
    List<Reservation> findAllByBranch(@Param("branchId") Long branchId);

    //------------------------receptionist query END---------------------
}
