package com.ByteKnights.com.resturarent_system.dto;

import jakarta.validation.constraints.Min;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTableRequest {

    @Min(value = 1, message = "Table number must be at least 1")
    private Integer tableNumber;

    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    private String status; // AVAILABLE, OCCUPIED, RESERVED
}
