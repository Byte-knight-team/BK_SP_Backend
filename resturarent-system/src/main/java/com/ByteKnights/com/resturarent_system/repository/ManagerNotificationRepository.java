package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.ManagerNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManagerNotificationRepository extends JpaRepository<ManagerNotification, Long> {

    List<ManagerNotification> findByBranchIdAndIsReadFalseOrderByCreatedAtDesc(Long branchId);
    
    long countByBranchIdAndIsReadFalse(Long branchId);
}
