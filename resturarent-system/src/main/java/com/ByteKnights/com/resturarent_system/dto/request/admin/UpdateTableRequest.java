package com.ByteKnights.com.resturarent_system.dto.request.admin;

import jakarta.validation.constraints.Min;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * Payload for partial table updates.
 */
public class UpdateTableRequest {

    @Min(value = 1, message = "Table number must be at least 1")
    private Integer tableNumber;

    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    // Allowed values: AVAILABLE, OCCUPIED, RESERVED.
    private String status;
}
