package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.request.receptionist.CreateReservationRequest;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReservationResponseDTO;

public interface ReceptionistReservationService {

    ReservationResponseDTO createReservation(CreateReservationRequest request, String userEmail);
}
