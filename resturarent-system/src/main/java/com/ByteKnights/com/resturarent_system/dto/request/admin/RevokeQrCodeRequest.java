package com.ByteKnights.com.resturarent_system.dto.request.admin;

import jakarta.validation.constraints.Size;
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
public class RevokeQrCodeRequest {

    @Size(max = 255, message = "Revoke reason must be at most 255 characters")
    private String revokedReason;
}
