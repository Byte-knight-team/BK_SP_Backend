package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.response.kitchen.ChefDashboardStatsDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.ChefDetailsDTO;
import com.ByteKnights.com.resturarent_system.entity.ChefWorkStatus;

import java.util.List;

public interface KitchenChefService {
    ChefDashboardStatsDTO getChefDashboardStats(String userEmail);
    List<ChefDetailsDTO> getChefDetailsToday(String chiefChefEmail);
    void checkInChef(Long chefId);
    void checkOutChef(Long chefId);
    void updateChefWorkStatus(Long chefId, ChefWorkStatus newStatus);
}
