package com.ByteKnights.com.resturarent_system.dto.request.receptionist;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Reason the receptionist gives when cancelling a reservation. The reason is stored on
 * the reservation and (for a paid booking) drives the refund decision.
 */
@Data
public class CancelReservationRequest {

    @NotBlank(message = "Cancel reason is required")
    private String reason;
}
