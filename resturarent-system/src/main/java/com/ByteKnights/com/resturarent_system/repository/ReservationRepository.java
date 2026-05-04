package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    //------------------------receptionist query START---------------------


    // Find a reservation that is CONFIRMED for a specific table
    @Query("SELECT r FROM Reservation r WHERE r.table.id = :tableId AND r.status = 'CONFIRMED'")
    Optional<Reservation> findActiveReservationByTableId(Long tableId);


    //------------------------receptionist query END---------------------
}
