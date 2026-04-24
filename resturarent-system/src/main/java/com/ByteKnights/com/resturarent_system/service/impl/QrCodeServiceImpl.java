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
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.security.JwtService;
import com.ByteKnights.com.resturarent_system.service.QrCodeService;
import com.ByteKnights.com.resturarent_system.util.QrCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
public class QrCodeServiceImpl implements QrCodeService {

    private final QrCodeRepository qrCodeRepository;
    private final RestaurantTableRepository tableRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${app.jwt.qr-token-expiration-ms:2400000}")
    private long qrTokenExpirationMs;

    @Value("${app.qr.scan-base-url:http://localhost:3000/scan}")
    private String qrScanBaseUrl;

    @Override
    @Transactional
    public QrCodeResponse createQrCode(Long tableId, Long actorUserId) {
        RestaurantTable table = findTableForUpdateOrThrow(tableId);
        validateTableHasBranch(table);
        User actorUser = findUserByIdOrThrow(actorUserId);

        if (qrCodeRepository.existsByTableIdAndActiveTrue(tableId)) {
            throw new DuplicateResourceException(
                    "Active QR code already exists for table " + tableId);
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
        QrCode qrCode = findQrCodeOrThrow(qrCodeId);

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
        QrCode existing = findQrCodeOrThrow(qrCodeId);
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
        QrCode qrCode = findQrCodeOrThrow(qrCodeId);
        if (!Boolean.TRUE.equals(qrCode.getActive())) {
            throw new InvalidOperationException("Cannot download image for a revoked QR code: " + qrCodeId);
        }

        SecureQrPayload payload = buildSecureQrPayload(qrCode);
        return payload.imageBytes();
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

        long remainingMs = Math.max(1, expiresAt.toEpochMilli() - Instant.now().toEpochMilli());
        return jwtService.generateQrToken(claims, "table-qr-" + qrCode.getId(), remainingMs);
    }

    private String buildQrUrl(String qrToken) {
        String separator = qrScanBaseUrl.contains("?") ? "&" : "?";
        String encodedToken = URLEncoder.encode(qrToken, StandardCharsets.UTF_8);
        return qrScanBaseUrl + separator + "qr_token=" + encodedToken;
    }

    private QrCodeResponse mapToResponse(QrCode qrCode) {
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
                .createdByUserId(qrCode.getCreatedByUser().getId())
                .createdAt(qrCode.getCreatedAt())
                .updatedAt(qrCode.getUpdatedAt())
                .build();
    }

    private record SecureQrPayload(String token, String url, byte[] imageBytes, String expiresAt) {
    }
}
