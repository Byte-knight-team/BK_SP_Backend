package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.Branch;
import com.ByteKnights.com.resturarent_system.entity.BranchConfig;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BranchConfigRepository extends JpaRepository<BranchConfig, Long> {
    Optional<BranchConfig> findByBranch(Branch branch);

    Optional<BranchConfig> findByBranchId(Long branchId);
}