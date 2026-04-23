package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.QrCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QrCodeRepository extends JpaRepository<QrCode, Long> {

    Optional<QrCode> findFirstByBranchIdAndTableIdAndActiveTrue(Long branchId, Long tableId);

    Optional<QrCode> findByIdAndActiveTrue(Long id);

    boolean existsByBranchIdAndTableIdAndActiveTrue(Long branchId, Long tableId);
}
