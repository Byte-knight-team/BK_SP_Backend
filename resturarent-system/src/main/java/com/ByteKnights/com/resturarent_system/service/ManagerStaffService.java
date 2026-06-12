package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.response.manager.ManagerStaffSummaryDTO;

public interface ManagerStaffService {
    ManagerStaffSummaryDTO getStaffSummary(Long targetBranchId, Long userId);
}
