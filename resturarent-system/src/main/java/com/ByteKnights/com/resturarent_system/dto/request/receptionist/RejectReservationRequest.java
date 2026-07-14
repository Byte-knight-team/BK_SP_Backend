package com.ByteKnights.com.resturarent_system.dto.request.receptionist;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Receptionist rejects a customer's REQUESTED reservation with a reason
 * (shown to the customer).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RejectReservationRequest {

    @NotBlank(message = "A reason is required to reject a reservation")
    private String reason;
}
