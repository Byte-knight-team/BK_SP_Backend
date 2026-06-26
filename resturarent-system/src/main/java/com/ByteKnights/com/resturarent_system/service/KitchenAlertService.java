package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.request.kitchen.CreateAlertRequestDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.ActiveAlertDTO;

import java.util.List;

public interface KitchenAlertService {
    void createKitchenAlert(CreateAlertRequestDTO dto, String userEmail);
    List<ActiveAlertDTO> getActiveAlerts(String userEmail);
    void resolveAlert(Long alertId, String userEmail);
}
