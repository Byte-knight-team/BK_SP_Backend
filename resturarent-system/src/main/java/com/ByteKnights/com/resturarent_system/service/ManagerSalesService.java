package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.response.manager.ManagerSalesSummaryDTO;

public interface ManagerSalesService {
    ManagerSalesSummaryDTO getSalesSummary(Long branchId);
}
