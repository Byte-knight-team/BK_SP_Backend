package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.ChefRequest;
import com.ByteKnights.com.resturarent_system.entity.ChefRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChefRequestRepository extends JpaRepository<ChefRequest, Long> {

    // Find requests for a branch
    List<ChefRequest> findByBranchIdOrderByCreatedAtDesc(Long branchId);

    // Find only pending requests for a branch
    List<ChefRequest> findByBranchIdAndStatus(Long branchId, ChefRequestStatus status);

    // Count pending requests for a branch
    long countByBranchIdAndStatus(Long branchId, ChefRequestStatus status);

}
