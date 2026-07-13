package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.StandardResponse;
import com.ByteKnights.com.resturarent_system.dto.request.receptionist.CancelReservationRequest;
import com.ByteKnights.com.resturarent_system.dto.request.receptionist.CheckAvailabilityRequest;
import com.ByteKnights.com.resturarent_system.dto.request.receptionist.ConfirmReservationRequest;
import com.ByteKnights.com.resturarent_system.dto.request.receptionist.RejectReservationRequest;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.CheckAvailabilityResponse;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReservationResponseDTO;
import com.ByteKnights.com.resturarent_system.service.ReceptionistReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * REST endpoints for the receptionist to manage reservations in their branch:
 * check availability, confirm/reject customer requests, seat arrived parties, and cancel.
 * All routes are under /api/v1/receptionist/reservations and require table permissions.
 */
@RestController
@RequestMapping("/api/v1/receptionist/reservations")
@CrossOrigin
@RequiredArgsConstructor
public class ReceptionistReservationController {

    private final ReceptionistReservationService receptionistReservationService;

    // For a given time slot, return every branch table tagged FREE / OCCUPIED / BLOCKED
    // so the receptionist can pick tables (used before confirming a request).
    @PostMapping("/check-availability")
    @PreAuthorize("hasAuthority('RECEPTIONIST_TABLE_VIEW')")
    public ResponseEntity<StandardResponse> checkAvailability(
            @Valid @RequestBody CheckAvailabilityRequest request,
            Principal principal) {

        CheckAvailabilityResponse response = receptionistReservationService
                .checkAvailability(request, principal.getName());

        return ResponseEntity.ok(new StandardResponse(200, "Availability checked", response));
    }

    // Upcoming reservations for the branch (the dashboard's "Upcoming Reservations" card).
    @GetMapping
    @PreAuthorize("hasAuthority('RECEPTIONIST_TABLE_VIEW')")
    public ResponseEntity<StandardResponse> getUpcomingReservations(Principal principal) {
        List<ReservationResponseDTO> list = receptionistReservationService.getUpcomingReservations(principal.getName());
        return ResponseEntity.ok(new StandardResponse(200, "Upcoming reservations fetched", list));
    }

    // All reservations for the branch (any status) — paged + filtered, for the Reservations page
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('RECEPTIONIST_TABLE_VIEW')")
    public ResponseEntity<StandardResponse> getAllReservations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Integer tableNumber,
            @RequestParam(required = false) String status,
            Principal principal) {
        var result = receptionistReservationService.getAllReservations(
                principal.getName(), page, size, date, tableNumber, status);
        return ResponseEntity.ok(new StandardResponse(200, "All reservations fetched", result));
    }

    // Approve a customer's REQUESTED reservation: assign tables → CONFIRMED (starts the pay clock)
    @PostMapping("/{reservationId}/confirm")
    @PreAuthorize("hasAuthority('RECEPTIONIST_TABLE_UPDATE')")
    public ResponseEntity<StandardResponse> confirmReservation(
            @PathVariable Long reservationId,
            @Valid @RequestBody ConfirmReservationRequest request,
            Principal principal) {

        receptionistReservationService.confirmReservation(reservationId, request, principal.getName());
        return ResponseEntity.ok(new StandardResponse(200, "Reservation confirmed", null));
    }

    // Reject a customer's REQUESTED reservation with a reason
    @PostMapping("/{reservationId}/reject")
    @PreAuthorize("hasAuthority('RECEPTIONIST_TABLE_UPDATE')")
    public ResponseEntity<StandardResponse> rejectReservation(
            @PathVariable Long reservationId,
            @Valid @RequestBody RejectReservationRequest request,
            Principal principal) {

        receptionistReservationService.rejectReservation(reservationId, request, principal.getName());
        return ResponseEntity.ok(new StandardResponse(200, "Reservation rejected", null));
    }

    // Seat the reserved party: occupy the table and mark the reservation completed
    @PostMapping("/{reservationId}/seat")
    @PreAuthorize("hasAuthority('RECEPTIONIST_TABLE_UPDATE')")
    public ResponseEntity<StandardResponse> seatReservation(
            @PathVariable Long reservationId,
            @RequestBody(required = false) java.util.Map<String, Integer> body,
            Principal principal) {

        Integer guestCount = body != null ? body.get("guestCount") : null;
        receptionistReservationService.seatReservation(reservationId, guestCount, principal.getName());
        return ResponseEntity.ok(new StandardResponse(200, "Reservation seated", null));
    }

    // The live reservation on a given table (CONFIRMED/PAID + current window) — lets the
    // table card show who's booked and offer a Seat action. Returns null if none.
    @GetMapping("/table/{tableId}")
    @PreAuthorize("hasAuthority('RECEPTIONIST_TABLE_VIEW')")
    public ResponseEntity<StandardResponse> getTableNextReservation(
            @PathVariable Long tableId,
            Principal principal) {
        ReservationResponseDTO dto = receptionistReservationService.getTableNextReservation(tableId, principal.getName());
        return ResponseEntity.ok(new StandardResponse(200, "Table reservation fetched", dto));
    }

    // Cancel a reservation with a reason — cancels the whole booking, frees its tables
    // (and refunds per the refund rules for a paid booking).
    @PatchMapping("/{reservationId}/cancel")
    @PreAuthorize("hasAuthority('RECEPTIONIST_TABLE_VIEW')")
    public ResponseEntity<StandardResponse> cancelReservation(
            @PathVariable Long reservationId,
            @Valid @RequestBody CancelReservationRequest request) {
        receptionistReservationService.cancelReservation(reservationId, request);
        return ResponseEntity.ok(new StandardResponse(200, "Reservation cancelled", null));
    }
}
