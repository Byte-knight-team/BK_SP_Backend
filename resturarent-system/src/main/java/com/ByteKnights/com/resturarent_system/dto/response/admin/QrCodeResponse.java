package com.ByteKnights.com.resturarent_system.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class QrCodeResponse {

    /**
     * DTO returned to admin clients representing a QR code and its secure
     * payload. Contains both database metadata (ids, timestamps) and the
     * rendered/encoded QR payload so admin UIs can present or download it.
     *
     * Notes:
     * - `qrToken` contains the signed JWT included in the QR URL.
     * - `qrUrl` is the frontend URL that accepts the `qr_token` query param.
     * - `qrImageBase64` contains PNG bytes encoded as Base64 for quick
     *   preview in web interfaces.
     */

    private Long id;
    // Branch that the QR belongs to
    private Long branchId;
    // Table id that the QR is assigned to
    private Long tableId;
    // Whether the QR is currently active (not revoked)
    private Boolean isActive;
    // When the QR was last generated
    private LocalDateTime lastGeneratedAt;
    // When the QR was revoked (if applicable)
    private LocalDateTime revokedAt;
    // Optional explanation for revocation
    private String revokedReason;
    // Signed JWT embedded in the QR URL (expires according to config)
    private String qrToken;
    // Full URL encoded into the QR image (frontend scan handler)
    private String qrUrl;
    // PNG bytes encoded as Base64 for easy preview in UI
    private String qrImageBase64;
    // ISO instant string for token expiry (informational)
    private String qrTokenExpiresAt;
    // Admin user id who created this QR
    private Long createdByUserId;
    // Entity timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
