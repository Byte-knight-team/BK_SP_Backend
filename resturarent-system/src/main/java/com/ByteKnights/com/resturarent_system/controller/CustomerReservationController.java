package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.request.customer.CustomerCreateReservationRequest;
import com.ByteKnights.com.resturarent_system.dto.response.customer.CustomerReservationResponse;
import com.ByteKnights.com.resturarent_system.dto.response.customer.ReservationChargeBreakdown;
import com.ByteKnights.com.resturarent_system.service.CustomerReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import java.util.List;

@RestController
@RequestMapping("/api/v1/customer/reservations")
@RequiredArgsConstructor
public class CustomerReservationController {

    private final CustomerReservationService customerReservationService;

    @PostMapping
    public ResponseEntity<CustomerReservationResponse> createReservation(
            @Valid @RequestBody CustomerCreateReservationRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(customerReservationService.createReservationRequest(request, email));
    }

    @GetMapping
    public ResponseEntity<Page<CustomerReservationResponse>> getMyReservations(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "requests") String tab) {
        return ResponseEntity
                .ok(customerReservationService.getMyReservations(authentication.getName(), page, size, tab));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerReservationResponse> getReservationById(
            @PathVariable Long id,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(customerReservationService.getReservationById(id, email));
    }

    public static class CancelRequest {
        private String reason;

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class PayRequest {
        private String transactionReference;

        public String getTransactionReference() {
            return transactionReference;
        }

        public void setTransactionReference(String transactionReference) {
            this.transactionReference = transactionReference;
        }
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelReservation(
            @PathVariable Long id,
            @RequestBody(required = false) CancelRequest payload,
            Authentication authentication) {
        String email = authentication.getName();
        String reason = (payload != null && payload.getReason() != null) ? payload.getReason()
                : "Cancelled by customer";
        customerReservationService.cancelMyReservation(id, reason, email);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/branches")
    public ResponseEntity<List<com.ByteKnights.com.resturarent_system.dto.BranchResponse>> getActiveReservationBranches() {
        return ResponseEntity.ok(customerReservationService.getActiveReservationBranches());
    }

    @GetMapping("/preview-charge")
    public ResponseEntity<ReservationChargeBreakdown> previewCharge(
            @RequestParam Long branchId,
            @RequestParam int guestCount,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ResponseEntity.ok(customerReservationService.previewCharge(branchId, guestCount, startTime, endTime));
    }

}
