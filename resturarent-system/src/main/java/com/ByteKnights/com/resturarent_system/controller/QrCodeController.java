package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.ApiResponse;
import com.ByteKnights.com.resturarent_system.dto.request.admin.RegenerateQrCodeRequest;
import com.ByteKnights.com.resturarent_system.dto.request.admin.RevokeQrCodeRequest;
import com.ByteKnights.com.resturarent_system.dto.response.admin.QrCodeResponse;
import com.ByteKnights.com.resturarent_system.exception.InvalidOperationException;
import com.ByteKnights.com.resturarent_system.security.JwtUserPrincipal;
import com.ByteKnights.com.resturarent_system.service.QrCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/admin", "/api/v1/admin"})
@RequiredArgsConstructor
public class QrCodeController {

    private static final Logger log = LoggerFactory.getLogger(QrCodeController.class);

    private final QrCodeService qrCodeService;

    @PostMapping("/tables/{tableId}/qr-codes")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<QrCodeResponse>> createQrCode(
            @PathVariable Long tableId,
            Authentication authentication
    ) {
        Long actorUserId = extractActorUserId(authentication);
        log.info("Create QR requested for tableId={}, actorUserId={}", tableId, actorUserId);
        QrCodeResponse response = qrCodeService.createQrCode(tableId, actorUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("QR code generated successfully", response));
    }

    @GetMapping("/tables/{tableId}/qr-codes/active")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<QrCodeResponse>> getActiveQrCodeForTable(@PathVariable Long tableId) {
        log.info("Get active QR requested for tableId={}", tableId);
        QrCodeResponse response = qrCodeService.getActiveQrCodeForTable(tableId);
        return ResponseEntity.ok(ApiResponse.success("Active QR code fetched successfully", response));
    }

    @PostMapping("/qr-codes/{qrCodeId}/revoke")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<QrCodeResponse>> revokeQrCode(
            @PathVariable Long qrCodeId,
            @Valid @RequestBody(required = false) RevokeQrCodeRequest request
    ) {
        String reason = request != null ? request.getRevokedReason() : null;
        QrCodeResponse response = qrCodeService.revokeQrCode(qrCodeId, reason);
        return ResponseEntity.ok(ApiResponse.success("QR code revoked successfully", response));
    }

    @PostMapping("/qr-codes/{qrCodeId}/regenerate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<QrCodeResponse>> regenerateQrCode(
            @PathVariable Long qrCodeId,
            @Valid @RequestBody(required = false) RegenerateQrCodeRequest request,
            Authentication authentication
    ) {
        Long actorUserId = extractActorUserId(authentication);
        String revokeReason = request != null ? request.getRevokeReason() : null;
        QrCodeResponse response = qrCodeService.regenerateQrCode(qrCodeId, actorUserId, revokeReason);
        return ResponseEntity.ok(ApiResponse.success("QR code regenerated successfully", response));
    }

    @GetMapping("/qr-codes/{qrCodeId}/download")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<byte[]> downloadQrCode(@PathVariable Long qrCodeId) {
        byte[] image = qrCodeService.downloadQrCodeImage(qrCodeId);
        String filename = "qr-code-" + qrCodeId + ".png";

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(image);
    }

    private Long extractActorUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new InvalidOperationException("Authenticated user context is required");
        }
        return principal.getUser().getId();
    }
}
