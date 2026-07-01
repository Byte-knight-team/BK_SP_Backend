package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReceptionistDashboardStatsDTO;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReceptionistOrderCountByTypeDTO;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReceptionistRevenueByTypeDTO;

import java.util.List;

public interface ReceptionistDashboardService {

    ReceptionistDashboardStatsDTO getDashboardStats(String userEmail);

    List<ReceptionistRevenueByTypeDTO> getRevenueByType(String userEmail);

    ReceptionistOrderCountByTypeDTO getOrderCountsByType(String userEmail);
}
