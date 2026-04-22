package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.Branch;
import com.ByteKnights.com.resturarent_system.entity.BranchOperatingHours;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BranchOperatingHoursRepository extends JpaRepository<BranchOperatingHours, Long> {
    List<BranchOperatingHours> findByBranch(Branch branch);
    void deleteByBranch(Branch branch);
}