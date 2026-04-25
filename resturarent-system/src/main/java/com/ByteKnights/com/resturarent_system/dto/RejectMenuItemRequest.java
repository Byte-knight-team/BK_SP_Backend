package com.ByteKnights.com.resturarent_system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RejectMenuItemRequest {

    @NotBlank(message = "Rejection reason is required")
    private String rejectionReason;

    private Long adminUserId;
}
