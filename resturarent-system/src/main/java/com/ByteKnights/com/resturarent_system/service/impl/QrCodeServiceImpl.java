package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.response.admin.QrCodeResponse;
import com.ByteKnights.com.resturarent_system.entity.QrCode;
import com.ByteKnights.com.resturarent_system.entity.RestaurantTable;
import com.ByteKnights.com.resturarent_system.entity.User;
import com.ByteKnights.com.resturarent_system.exception.DuplicateResourceException;
import com.ByteKnights.com.resturarent_system.exception.InvalidOperationException;
import com.ByteKnights.com.resturarent_system.exception.ResourceNotFoundException;
import com.ByteKnights.com.resturarent_system.repository.QrCodeRepository;
import com.ByteKnights.com.resturarent_system.repository.RestaurantTableRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.security.JwtService;
import com.ByteKnights.com.resturarent_system.security.JwtUserPrincipal;
import com.ByteKnights.com.resturarent_system.service.QrCodeService;
import com.ByteKnights.com.resturarent_system.util.QrCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
/**
 * Service implementing QR code lifecycle operations.
 *
 * Responsibilities:
 * - Create, revoke and regenerate QR code records in the database.
 * - Build a secure QR payload (short URL containing a JWT token), render the
 *   QR image bytes and return a DTO suitable for admin UI consumption.
 *
 * Important notes:
 * - The QR image is purely a rendering of a URL that contains a signed JWT
 *   (`qr_token`). Token expiry is controlled by `app.jwt.qr-token-expiration-ms`.
 * - Revocation is enforced via the `QrCode.active` flag in the database;
 *   even if a token is unexpired, a revoked QR is considered invalid.
 */
public class QrCodeServiceImpl implements QrCodeService {

    private final QrCodeRepository qrCodeRepository;
    private final RestaurantTableRepository tableRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final JwtService jwtService;

    @Value("${app.jwt.qr-token-expiration-ms:2400000}")
    private long qrTokenExpirationMs;

    @Value("${app.qr.scan-base-url:http://localhost:5173/scan}")
    private String qrScanBaseUrl;

    @Override
    @Transactional
    public QrCodeResponse createQrCode(Long tableId, Long actorUserId) {

        /* 
            Creates and persists a new QrCode entity for the given table.
            If an active QR already exists, the existing active QR is returned
            (idempotent behavior for repeated create requests).
        */
       
        RestaurantTable table = findTableForUpdateOrThrow(tableId);
        validateTableHasBranch(table);
        enforceAdminBranchAccess(table.getBranch().getId());
        User actorUser = findUserByIdOrThrow(actorUserId);

        QrCode activeQr = qrCodeRepository.findFirstByTableIdAndActiveTrue(tableId).orElse(null);
        if (activeQr != null) {
            return mapToResponseWithSecureQr(activeQr);
        }

        QrCode qrCode = QrCode.builder()
                .branch(table.getBranch())
                .table(table)
                .active(true)
                .lastGeneratedAt(LocalDateTime.now())
            .createdByUser(actorUser)
                .build();

        QrCode saved = qrCodeRepository.save(qrCode);
        return mapToResponseWithSecureQr(saved);
    }

    @Override
    @Transactional
    public QrCodeResponse revokeQrCode(Long qrCodeId, String revokedReason) {

        /*
            Mark an existing QR code as revoked. This makes the QR immediately
            inactive for scanning even if the previously issued JWT token
            would otherwise remain valid until its `exp` time.
        */

        QrCode qrCode = findQrCodeOrThrow(qrCodeId);
        enforceAdminBranchAccess(qrCode.getBranch().getId());

        if (!Boolean.TRUE.equals(qrCode.getActive())) {
            throw new InvalidOperationException("QR code is already revoked: " + qrCodeId);
        }

        qrCode.setActive(false);
        qrCode.setRevokedAt(LocalDateTime.now());
        qrCode.setRevokedReason(resolveReason(revokedReason, "Revoked by admin"));

        QrCode updated = qrCodeRepository.save(qrCode);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public QrCodeResponse regenerateQrCode(Long qrCodeId, Long actorUserId, String revokeReason) {

        // Safely replace an active QR with a newly generated one. The
        // operation revokes the old QR and creates a replacement bound to
        // the same table and branch. This avoids token reuse after rotation.

        QrCode existing = findQrCodeOrThrow(qrCodeId);
        enforceAdminBranchAccess(existing.getBranch().getId());
        User actorUser = findUserByIdOrThrow(actorUserId);
        RestaurantTable lockedTable = findTableForUpdateOrThrow(existing.getTable().getId());
        validateTableHasBranch(lockedTable);

        if (!Boolean.TRUE.equals(existing.getActive())) {
            throw new InvalidOperationException("Cannot regenerate a revoked QR code: " + qrCodeId);
        }

        qrCodeRepository.findFirstByTableIdAndActiveTrue(existing.getTable().getId())
                .ifPresent(activeQr -> {
                    if (!activeQr.getId().equals(existing.getId())) {
                        throw new DuplicateResourceException(
                                "Another active QR code exists for table " + existing.getTable().getId());
                    }
                });

        existing.setActive(false);
        existing.setRevokedAt(LocalDateTime.now());
        existing.setRevokedReason(resolveReason(revokeReason, "Revoked due to regeneration"));
        qrCodeRepository.save(existing);

        QrCode replacement = QrCode.builder()
                .branch(existing.getBranch())
                .table(lockedTable)
                .active(true)
                .lastGeneratedAt(LocalDateTime.now())
            .createdByUser(actorUser)
                .build();

        QrCode saved = qrCodeRepository.save(replacement);
        return mapToResponseWithSecureQr(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadQrCodeImage(Long qrCodeId) {

        // Produce PNG bytes for the active QR's URL. Throws if QR is
        // revoked or the caller lacks admin access to the branch.

        QrCode qrCode = findQrCodeOrThrow(qrCodeId);
        enforceAdminBranchAccess(qrCode.getBranch().getId());
        if (!Boolean.TRUE.equals(qrCode.getActive())) {
            throw new InvalidOperationException("Cannot download image for a revoked QR code: " + qrCodeId);
        }

        SecureQrPayload payload = buildSecureQrPayload(qrCode);
        return payload.imageBytes();
    }

    @Override
    @Transactional(readOnly = true)
    public QrCodeResponse getActiveQrCodeForTable(Long tableId) {

        // Retrieve the active QR for a table and return a DTO that
        // includes a secure payload and base64 image for easy UI display.

        QrCode activeQr = qrCodeRepository.findFirstByTableIdAndActiveTrue(tableId)
                .orElseThrow(() -> new ResourceNotFoundException("No active QR code found for table: " + tableId));
        
        enforceAdminBranchAccess(activeQr.getBranch().getId());
        
        return mapToResponseWithSecureQr(activeQr);
    }

    private RestaurantTable findTableForUpdateOrThrow(Long tableId) {
        return tableRepository.findByIdForUpdate(tableId)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found with id: " + tableId));
    }

    private User findUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private QrCode findQrCodeOrThrow(Long qrCodeId) {
        return qrCodeRepository.findById(qrCodeId)
                .orElseThrow(() -> new ResourceNotFoundException("QR code not found with id: " + qrCodeId));
    }

    private void validateTableHasBranch(RestaurantTable table) {
        if (table.getBranch() == null || table.getBranch().getId() == null) {
            throw new InvalidOperationException("Table " + table.getId() + " is not assigned to a branch");
        }
    }

    private String resolveReason(String reason, String fallback) {
        if (reason == null || reason.isBlank()) {
            return fallback;
        }
        return reason;
    }

    private QrCodeResponse mapToResponseWithSecureQr(QrCode qrCode) {
        SecureQrPayload payload = buildSecureQrPayload(qrCode);
        String qrImageBase64 = Base64.getEncoder().encodeToString(payload.imageBytes());

        return mapToResponse(qrCode).toBuilder()
                .qrToken(payload.token())
                .qrUrl(payload.url())
                .qrImageBase64(qrImageBase64)
                .qrTokenExpiresAt(payload.expiresAt())
                .build();
    }

    private SecureQrPayload buildSecureQrPayload(QrCode qrCode) {

        // Build the secure payload included in responses. The payload
        // contains:
        // - a signed `qr_token` JWT with expiry set from configuration
        // - a `qrUrl` that appends the token as a query parameter
        // - rendered PNG bytes for the QR image

        Instant expiresAt = Instant.now().plusMillis(qrTokenExpirationMs);
        String qrToken = generateQrToken(qrCode, expiresAt);
        String qrUrl = buildQrUrl(qrToken);
        byte[] qrImageBytes = QrCodeGenerator.generateQRCodeImage(qrUrl);
        String expiresAtIso = DateTimeFormatter.ISO_INSTANT.format(expiresAt.atOffset(ZoneOffset.UTC));
        return new SecureQrPayload(qrToken, qrUrl, qrImageBytes, expiresAtIso);
    }

    private String generateQrToken(QrCode qrCode, Instant expiresAt) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("qr_id", qrCode.getId());
        claims.put("branch_id", qrCode.getBranch().getId());
        claims.put("table_id", qrCode.getTable().getId());
        claims.put("token_type", "table_qr");

        // Calculate remaining milliseconds until the chosen expiry instant
        // and delegate JWT assembly to `JwtService`. The resulting token
        // contains the `exp` claim and is signed with the application key.

        long remainingMs = Math.max(1, expiresAt.toEpochMilli() - Instant.now().toEpochMilli());
        return jwtService.generateQrToken(claims, "table-qr-" + qrCode.getId(), remainingMs);
    }

    private String buildQrUrl(String qrToken) {
        String separator = qrScanBaseUrl.contains("?") ? "&" : "?";
        String encodedToken = URLEncoder.encode(qrToken, StandardCharsets.UTF_8);
        return qrScanBaseUrl + separator + "qr_token=" + encodedToken;
    }

    private QrCodeResponse mapToResponse(QrCode qrCode) {
        Long createdByUserId = qrCode.getCreatedByUser() != null
            ? qrCode.getCreatedByUser().getId()
            : null;

        return QrCodeResponse.builder()
                .id(qrCode.getId())
                .branchId(qrCode.getBranch().getId())
                .tableId(qrCode.getTable().getId())
                .isActive(qrCode.getActive())
                .lastGeneratedAt(qrCode.getLastGeneratedAt())
                .revokedAt(qrCode.getRevokedAt())
                .revokedReason(qrCode.getRevokedReason())
                .qrToken(null)
                .qrUrl(null)
                .qrImageBase64(null)
                .qrTokenExpiresAt(null)
                .createdByUserId(createdByUserId)
                .createdAt(qrCode.getCreatedAt())
                .updatedAt(qrCode.getUpdatedAt())
                .build();
    }

    private void enforceAdminBranchAccess(Long targetBranchId) {
        Long adminBranchId = resolveCurrentAdminBranchIdOrNull();

        if (adminBranchId != null && !adminBranchId.equals(targetBranchId)) {
            throw new InvalidOperationException("ADMIN can access QR codes only in their own branch");
        }
    }

    private Long resolveCurrentAdminBranchIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getAuthorities() == null) {
            return null;
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (!isAdmin) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof JwtUserPrincipal jwtUser)
                || jwtUser.getUser() == null
                || jwtUser.getUser().getId() == null) {
            throw new InvalidOperationException("Authenticated ADMIN user not found");
        }

        return staffRepository.findByUserId(jwtUser.getUser().getId())
                .map(staff -> staff.getBranch().getId())
                .orElseThrow(() -> new InvalidOperationException("Admin staff profile not found"));
    }

    private record SecureQrPayload(String token, String url, byte[] imageBytes, String expiresAt) {
    }
}
