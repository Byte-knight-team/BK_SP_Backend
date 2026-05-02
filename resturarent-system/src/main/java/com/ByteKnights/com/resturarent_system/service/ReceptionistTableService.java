package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.request.receptionist.CreateReservationRequest;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReceptionistTableResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public interface ReceptionistTableService {

    List<ReceptionistTableResponse> getBranchTables(String userEmail);

    void occupyTable(Long tableId, Integer guestCount, String userEmail);

    void clearTable(Long tableId, String userEmail);

    void createReservation(CreateReservationRequest request, String userEmail);

    void checkInGuest(Long tableId, String userEmail);

    void cancelReservation(Long tableId, String reason, String userEmail);
}
