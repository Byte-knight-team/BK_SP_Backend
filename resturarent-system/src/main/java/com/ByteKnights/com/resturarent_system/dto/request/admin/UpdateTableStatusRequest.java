package com.ByteKnights.com.resturarent_system.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * Payload for status-only updates.
 */
public class UpdateTableStatusRequest {

    @NotBlank(message = "Status is required")
    // Allowed values: AVAILABLE, OCCUPIED, RESERVED.
    private String status;
}
