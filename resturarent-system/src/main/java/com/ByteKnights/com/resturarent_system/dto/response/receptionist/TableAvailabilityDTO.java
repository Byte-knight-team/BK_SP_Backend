package com.ByteKnights.com.resturarent_system.dto.response.receptionist;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableAvailabilityDTO {

    private Long tableId;
    private Integer tableNumber;
    private Integer capacity;

    // FREE | RESERVED | OCCUPIED
    private String status;

    // populated only when status == RESERVED (the clashing reservation's window)
    private LocalDateTime conflictStart;
    private LocalDateTime conflictEnd;

    // true when the clash isn't a direct time overlap but the required 1-hour gap between
    // reservations (the requested slot is within an hour of the clashing reservation)
    private boolean gapConflict;

    // populated only when status == OCCUPIED (so the receptionist can judge manually)
    private LocalDateTime occupiedSince;
    private Integer activeOrderCount;

    // populated when the OCCUPIED table was seated from a reservation — its reserved window,
    // so the receptionist knows when it should free up
    private LocalDateTime occupiedReservationStart;
    private LocalDateTime occupiedReservationEnd;
}
