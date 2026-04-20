package com.ByteKnights.com.resturarent_system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTableStatusRequest {

    @NotBlank(message = "Status is required")
    private String status; // AVAILABLE, OCCUPIED, RESERVED
}
