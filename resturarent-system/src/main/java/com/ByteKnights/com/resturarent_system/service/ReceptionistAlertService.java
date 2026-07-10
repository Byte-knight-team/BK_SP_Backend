package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.response.kitchen.ActiveAlertDTO;

import java.util.List;

public interface ReceptionistAlertService {

    List<ActiveAlertDTO> getAlerts(String userEmail);
}
