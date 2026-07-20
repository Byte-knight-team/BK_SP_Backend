package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {

    // Find all active vendors for a branch (for dropdowns and the vendor directory table)
    List<Vendor> findByBranchIdAndIsActiveTrueOrderByNameAsc(Long branchId);

    // Find ALL vendors (including deactivated) — used for admin views and audit history
    List<Vendor> findByBranchIdOrderByNameAsc(Long branchId);

    // Count active vendors for a branch — used in the procurement summary card
    long countByBranchIdAndIsActiveTrue(Long branchId);
}
