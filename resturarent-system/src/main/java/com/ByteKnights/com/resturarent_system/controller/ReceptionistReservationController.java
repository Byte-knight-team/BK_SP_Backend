package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.StandardResponse;
import com.ByteKnights.com.resturarent_system.dto.request.receptionist.CreateReservationRequest;
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

@RestController
@RequestMapping("/api/v1/receptionist/reservations")
@CrossOrigin
@RequiredArgsConstructor
public class ReceptionistReservationController {

    private final ReceptionistReservationService receptionistReservationService;

    @PostMapping
    @PreAuthorize("hasAuthority('RECEPTIONIST_TABLE_VIEW')")
    public ResponseEntity<StandardResponse> createReservation(
            @Valid @RequestBody CreateReservationRequest request,
            Principal principal) {

        ReservationResponseDTO response = receptionistReservationService
                .createReservation(request, principal.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new StandardResponse(201, "Reservation created successfully", response)
        );
    }

    @GetMapping
    @PreAuthorize("hasAuthority('RECEPTIONIST_TABLE_VIEW')")
    public ResponseEntity<StandardResponse> getUpcomingReservations(Principal principal) {
        List<ReservationResponseDTO> list = receptionistReservationService.getUpcomingReservations(principal.getName());
        return ResponseEntity.ok(new StandardResponse(200, "Upcoming reservations fetched", list));
    }

    @GetMapping("/table/{tableId}")
    @PreAuthorize("hasAuthority('RECEPTIONIST_TABLE_VIEW')")
    public ResponseEntity<StandardResponse> getTableNextReservation(
            @PathVariable Long tableId,
            Principal principal) {
        ReservationResponseDTO dto = receptionistReservationService.getTableNextReservation(tableId, principal.getName());
        return ResponseEntity.ok(new StandardResponse(200, "Table reservation fetched", dto));
    }
}
