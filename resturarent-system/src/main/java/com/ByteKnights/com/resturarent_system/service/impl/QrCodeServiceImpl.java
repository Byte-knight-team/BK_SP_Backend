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
import com.ByteKnights.com.resturarent_system.service.QrCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class QrCodeServiceImpl implements QrCodeService {

    private final QrCodeRepository qrCodeRepository;
    private final RestaurantTableRepository tableRepository;
    private final UserRepository userRepository;

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
        return mapToResponse(saved);
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
        return mapToResponse(saved);
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

    private QrCodeResponse mapToResponse(QrCode qrCode) {
        return QrCodeResponse.builder()
                .id(qrCode.getId())
                .branchId(qrCode.getBranch().getId())
                .tableId(qrCode.getTable().getId())
                .isActive(qrCode.getActive())
                .lastGeneratedAt(qrCode.getLastGeneratedAt())
                .revokedAt(qrCode.getRevokedAt())
                .revokedReason(qrCode.getRevokedReason())
                .createdByUserId(qrCode.getCreatedByUser().getId())
                .createdAt(qrCode.getCreatedAt())
                .updatedAt(qrCode.getUpdatedAt())
                .build();
    }
}
