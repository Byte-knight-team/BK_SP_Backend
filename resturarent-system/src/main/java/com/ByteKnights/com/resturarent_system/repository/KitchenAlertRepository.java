package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.KitchenAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KitchenAlertRepository extends JpaRepository<KitchenAlert, Long> {

    List<KitchenAlert> findByBranchIdAndIsResolvedFalseOrderByCreatedAtDesc(Long branchId);
    /*
    no need to write a query like this
        SELECT * FROM kitchen_alerts
        WHERE branch_id = ?
        AND is_resolved = false
        ORDER BY created_at DESC;
     */
}
