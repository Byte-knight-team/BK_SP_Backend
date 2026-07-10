package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.request.receptionist.CancelReservationRequest;
import com.ByteKnights.com.resturarent_system.dto.request.receptionist.CheckAvailabilityRequest;
import com.ByteKnights.com.resturarent_system.dto.request.receptionist.CreateReservationRequest;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.CheckAvailabilityResponse;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReservationResponseDTO;

import java.util.List;

public interface ReceptionistReservationService {

    ReservationResponseDTO createReservation(CreateReservationRequest request, String userEmail);

    CheckAvailabilityResponse checkAvailability(CheckAvailabilityRequest request, String userEmail);

    List<ReservationResponseDTO> getUpcomingReservations(String userEmail);

    List<ReservationResponseDTO> getAllReservations(String userEmail);

    void seatReservation(Long reservationId, Integer guestCount, String userEmail);

    ReservationResponseDTO getTableNextReservation(Long tableId, String userEmail);

    void cancelReservation(Long reservationId, CancelReservationRequest request);
}
