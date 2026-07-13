package com.ByteKnights.com.resturarent_system.dto.response.receptionist;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A compact reservation summary shown on a table card/modal (who booked it and the
 * reserved window) — used for "today's reservations" and the currently-seated reservation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableReservationSummary {

    private Long reservationId;
    private String customerName;
    private String customerPhone;
    private LocalDateTime reservationTime;
    private LocalDateTime endTime;
}
