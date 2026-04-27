package com.ByteKnights.com.resturarent_system.dto.request.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * Payload for creating a new table.
 */
public class CreateTableRequest {

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    @NotNull(message = "Table number is required")
    @Min(value = 1, message = "Table number must be at least 1")
    private Integer tableNumber;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    // Optional; defaults to AVAILABLE when omitted.
    private String status;
}
