package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.request.receptionist.CancelReservationRequest;
import com.ByteKnights.com.resturarent_system.dto.request.receptionist.CheckAvailabilityRequest;
import com.ByteKnights.com.resturarent_system.dto.request.receptionist.ConfirmReservationRequest;
import com.ByteKnights.com.resturarent_system.dto.request.receptionist.CreateReservationRequest;
import com.ByteKnights.com.resturarent_system.dto.request.receptionist.RejectReservationRequest;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.CheckAvailabilityResponse;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.PagedResponse;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReservationResponseDTO;

import java.util.List;

/**
 * Reservation lifecycle from the receptionist's side: review requests (confirm/reject),
 * check availability, seat the arrived party, and cancel. Scoped to the receptionist's branch.
 */
public interface ReceptionistReservationService {

    ReservationResponseDTO createReservation(CreateReservationRequest request, String userEmail);

    CheckAvailabilityResponse checkAvailability(CheckAvailabilityRequest request, String userEmail);

    // Receptionist approves a customer's REQUESTED reservation: assign tables → CONFIRMED (starts pay clock).
    void confirmReservation(Long reservationId, ConfirmReservationRequest request, String userEmail);

    // Receptionist rejects a customer's REQUESTED reservation with a reason.
    void rejectReservation(Long reservationId, RejectReservationRequest request, String userEmail);

    List<ReservationResponseDTO> getUpcomingReservations(String userEmail);

    PagedResponse<ReservationResponseDTO> getAllReservations(
            String userEmail, int page, int size, String date, Integer tableNumber, String status);

    void seatReservation(Long reservationId, Integer guestCount, String userEmail);

    ReservationResponseDTO getTableNextReservation(Long tableId, String userEmail);

    void cancelReservation(Long reservationId, CancelReservationRequest request);
}
