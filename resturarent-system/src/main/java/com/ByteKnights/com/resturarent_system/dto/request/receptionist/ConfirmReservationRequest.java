package com.ByteKnights.com.resturarent_system.dto.request.receptionist;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

/**
 * Receptionist confirms a customer's REQUESTED reservation:
 * assigns one or more tables and optionally adds a note.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmReservationRequest {

    // Table numbers the receptionist assigns to this booking (at least one).
    @NotEmpty(message = "At least one table must be assigned to confirm the reservation")
    private List<Integer> tableNumbers;

    // Optional note from the receptionist to the customer.
    private String note;
}
