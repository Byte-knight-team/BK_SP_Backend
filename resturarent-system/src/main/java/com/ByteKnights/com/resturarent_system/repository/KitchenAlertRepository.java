package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.KitchenAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KitchenAlertRepository extends JpaRepository<KitchenAlert, Long> {

    // We will use this later to fetch alerts for a specific branch
    List<KitchenAlert> findByBranchIdAndIsResolvedFalseOrderByCreatedAtDesc(Long branchId);
}
