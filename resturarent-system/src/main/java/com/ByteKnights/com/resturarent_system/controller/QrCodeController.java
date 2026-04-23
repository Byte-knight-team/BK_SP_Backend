package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.request.admin.RegenerateQrCodeRequest;
import com.ByteKnights.com.resturarent_system.dto.request.admin.RevokeQrCodeRequest;
import com.ByteKnights.com.resturarent_system.dto.response.admin.QrCodeResponse;
import com.ByteKnights.com.resturarent_system.exception.InvalidOperationException;
import com.ByteKnights.com.resturarent_system.security.JwtUserPrincipal;
import com.ByteKnights.com.resturarent_system.service.QrCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class QrCodeController {

    private final QrCodeService qrCodeService;

    @PostMapping("/branches/{branchId}/tables/{tableId}/qr-codes")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<QrCodeResponse> createQrCode(
            @PathVariable Long branchId,
            @PathVariable Long tableId,
            Authentication authentication
    ) {
        Long actorUserId = extractActorUserId(authentication);
        QrCodeResponse response = qrCodeService.createQrCode(branchId, tableId, actorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/qr-codes/{qrCodeId}/revoke")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<QrCodeResponse> revokeQrCode(
            @PathVariable Long qrCodeId,
            @Valid @RequestBody(required = false) RevokeQrCodeRequest request
    ) {
        String reason = request != null ? request.getRevokedReason() : null;
        QrCodeResponse response = qrCodeService.revokeQrCode(qrCodeId, reason);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/qr-codes/{qrCodeId}/regenerate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<QrCodeResponse> regenerateQrCode(
            @PathVariable Long qrCodeId,
            @Valid @RequestBody(required = false) RegenerateQrCodeRequest request,
            Authentication authentication
    ) {
        Long actorUserId = extractActorUserId(authentication);
        String revokeReason = request != null ? request.getRevokeReason() : null;
        QrCodeResponse response = qrCodeService.regenerateQrCode(qrCodeId, actorUserId, revokeReason);
        return ResponseEntity.ok(response);
    }

    private Long extractActorUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            throw new InvalidOperationException("Authenticated user context is required");
        }
        return principal.getUser().getId();
    }
}
