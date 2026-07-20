package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.request.customer.CustomerCreateReservationRequest;
import com.ByteKnights.com.resturarent_system.dto.response.customer.CustomerReservationResponse;
import com.ByteKnights.com.resturarent_system.dto.response.customer.ReservationChargeBreakdown;
import org.springframework.data.domain.Page;
import java.time.LocalDateTime;
import java.util.List;

public interface CustomerReservationService {

    CustomerReservationResponse createReservationRequest(CustomerCreateReservationRequest request, String customerEmail);

    Page<CustomerReservationResponse> getMyReservations(String customerEmail, int page, int size, String tab);

    CustomerReservationResponse getReservationById(Long reservationId, String customerEmail);

    void cancelMyReservation(Long reservationId, String reason, String customerEmail);

    void webhookPayReservation(Long reservationId, String transactionRef);

    ReservationChargeBreakdown previewCharge(Long branchId, int guestCount, LocalDateTime start, LocalDateTime end);

    List<com.ByteKnights.com.resturarent_system.dto.BranchResponse> getActiveReservationBranches();
}
