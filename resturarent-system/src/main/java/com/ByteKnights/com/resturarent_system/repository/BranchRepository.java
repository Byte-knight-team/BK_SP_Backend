package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.Branch;
import com.ByteKnights.com.resturarent_system.entity.BranchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    Optional<Branch> findByIdAndStatus(Long id, BranchStatus status);

    boolean existsByNameIgnoreCase(String name);

    Optional<Branch> findByNameIgnoreCase(String name);
}