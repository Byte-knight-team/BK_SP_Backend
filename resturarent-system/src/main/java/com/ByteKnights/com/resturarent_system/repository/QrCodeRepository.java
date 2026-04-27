package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.QrCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Persistence queries for QR code lifecycle operations.
 */
public interface QrCodeRepository extends JpaRepository<QrCode, Long> {

    /**
     * Finds the active QR for a table, if one exists.
     */
    Optional<QrCode> findFirstByTableIdAndActiveTrue(Long tableId);

    /**
     * Finds a QR by id only when the QR is active.
     */
    Optional<QrCode> findByIdAndActiveTrue(Long id);

    /**
     * Checks whether a table currently has an active QR.
     */
    boolean existsByTableIdAndActiveTrue(Long tableId);

    /**
     * Checks whether any QR history exists for the table (active or revoked).
     *
     * This is used by table deletion rules to block removal when QR references
     * are present, preventing foreign key violations and preserving QR history.
     */
    boolean existsByTableId(Long tableId);
}
