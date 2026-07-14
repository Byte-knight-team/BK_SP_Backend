package com.ByteKnights.com.resturarent_system.dto.response.receptionist;

import com.ByteKnights.com.resturarent_system.entity.TableStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Everything the Table Management grid/modal needs for one table: its live state,
 * current guests, active orders on it, today's reservations, and (if seated from a
 * reservation) that reservation's details.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ReceptionistTableResponse {
    private Long id;
    private Integer tableNumber;
    private Integer capacity;
    private TableStatus status;               // AVAILABLE / RESERVED / OCCUPIED
    private Integer currentGuestCount;
    private Integer activeOrderCount;
    private LocalDateTime statusUpdatedAt;    // drives the "occupied for X min" timer
    private List<TableOrderSummary> activeOrders;         // orders currently on this table
    private List<TableReservationSummary> todayReservations; // today's bookings for this table

    // Set only when the table is OCCUPIED because a reservation was seated — carries that
    // reservation's window so the card/modal can show "Occupied for reservation …" and blink at end time.
    private TableReservationSummary seatedReservation;
}
