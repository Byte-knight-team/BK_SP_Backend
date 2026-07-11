package com.ByteKnights.com.resturarent_system.dto.request.receptionist;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CancelReservationRequest {

    @NotBlank(message = "Cancel reason is required")
    private String reason;
}
