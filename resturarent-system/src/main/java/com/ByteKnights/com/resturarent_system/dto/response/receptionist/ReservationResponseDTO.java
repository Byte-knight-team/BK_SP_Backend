package com.ByteKnights.com.resturarent_system.dto.response.receptionist;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The receptionist-facing view of a reservation (one row on the Reservations page).
 * Flattens the entity: the many-to-many tables become simple id/number lists.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponseDTO {

    private Long id;
    // A booking can cover multiple tables under one customer.
    private List<Long> tableIds;        // table primary keys
    private List<Integer> tableNumbers; // human-facing table numbers (e.g. 9, 11)
    private String customerName;
    private String customerPhone;
    private LocalDateTime reservationTime; // slot start
    private LocalDateTime endTime;         // slot end
    private Integer guestCount;
    private String notes;
    // One of the ReservationStatus values: REQUESTED / CONFIRMED / REJECTED / PAID / EXPIRED / CANCELLED / COMPLETED
    private String status;
    private LocalDateTime createdAt;
}
