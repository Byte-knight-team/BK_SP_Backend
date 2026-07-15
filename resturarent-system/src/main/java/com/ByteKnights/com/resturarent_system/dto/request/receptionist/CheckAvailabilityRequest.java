package com.ByteKnights.com.resturarent_system.dto.request.receptionist;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Sent by the receptionist to ask "for this time slot, which tables are free?".
 * The backend tags every branch table as FREE / OCCUPIED / BLOCKED for this window
 * (see checkAvailability). Used before assigning tables to a booking.
 */
@Data
public class CheckAvailabilityRequest {

    // Start of the requested slot.
    @NotNull(message = "Reservation time is required")
    private LocalDateTime reservationTime;

    // End of the requested slot (must be after the start).
    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    // Party size — used to hint whether the free tables can seat everyone.
    @NotNull(message = "Guest count is required")
    private Integer guestCount;
}
