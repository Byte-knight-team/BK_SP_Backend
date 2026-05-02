package com.ByteKnights.com.resturarent_system.dto.request.receptionist;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelReservationRequest {
    @NotBlank(message = "Cancellation reason is required")
    private String reason;
}
