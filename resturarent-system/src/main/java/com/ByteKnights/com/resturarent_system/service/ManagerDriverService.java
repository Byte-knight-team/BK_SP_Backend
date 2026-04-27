package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.response.manager.ManagerDriverSummaryDTO;

public interface ManagerDriverService {
    ManagerDriverSummaryDTO getDriverSummary(Long branchId, Long userId);
}
