package com.ByteKnights.com.resturarent_system.dto.response.receptionist;

import com.ByteKnights.com.resturarent_system.entity.TableStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ReceptionistTableResponse {
    private Long id;
    private Integer tableNumber;
    private Integer capacity;
    private TableStatus status;
    private Integer currentGuestCount;
    private Integer activeOrderCount;
    private LocalDateTime statusUpdatedAt;
    private List<TableOrderSummary> activeOrders;
    private List<TableReservationSummary> todayReservations;

    // Set only when the table is OCCUPIED because a reservation was seated — carries that
    // reservation's window so the card/modal can show "Occupied for reservation …" and blink at end time.
    private TableReservationSummary seatedReservation;
}
