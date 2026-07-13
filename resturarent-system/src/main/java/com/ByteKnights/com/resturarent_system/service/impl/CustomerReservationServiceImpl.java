package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.customer.CustomerCreateReservationRequest;
import com.ByteKnights.com.resturarent_system.dto.response.customer.CustomerReservationResponse;
import com.ByteKnights.com.resturarent_system.dto.response.customer.ReservationChargeBreakdown;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.*;
import com.ByteKnights.com.resturarent_system.service.CustomerReservationService;
import com.ByteKnights.com.resturarent_system.service.WebSocketNotificationService;
import com.ByteKnights.com.resturarent_system.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerReservationServiceImpl implements CustomerReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationPaymentRepository reservationPaymentRepository;
    private final BranchRepository branchRepository;
    private final BranchConfigRepository branchConfigRepository;
    private final RestaurantTableRepository restaurantTableRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final WebSocketNotificationService webSocketNotificationService;
    private final EmailService emailService;

    @org.springframework.beans.factory.annotation.Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    @Transactional
    public CustomerReservationResponse createReservationRequest(CustomerCreateReservationRequest request,
            String customerEmail) {
        User user = userRepository.findByEmail(customerEmail).orElseThrow(() -> new RuntimeException("User not found"));
        Customer customer = customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Branch not found"));
        BranchConfig config = branchConfigRepository.findByBranchId(branch.getId())
                .orElseThrow(() -> new RuntimeException("Branch config not found"));

        if (!config.isBranchActiveForOrders() || !config.isDineInEnabled() || !config.isReservationsEnabled()) {
            throw new RuntimeException("Reservations are currently unavailable for this branch");
        }

        if (request.getGuestCount() > config.getReservationMaxGuestCount()) {
            throw new RuntimeException(
                    "Guest count exceeds the maximum allowed (" + config.getReservationMaxGuestCount() + ")");
        }

        if (request.getStartTime().isBefore(LocalDateTime.now().plusHours(config.getReservationMinLeadHours()))) {
            throw new RuntimeException(
                    "Reservations must be made at least " + config.getReservationMinLeadHours() + " hours in advance");
        }

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new RuntimeException("End time must be after start time");
        }

        ReservationChargeBreakdown chargeBreakdown = calculateCharge(request.getGuestCount(), request.getStartTime(),
                request.getEndTime(), config);

        Reservation reservation = Reservation.builder()
                .branch(branch)
                .customer(customer)
                .customerName(user.getFullName())
                .customerPhone(user.getPhone())
                .reservationTime(request.getStartTime())
                .endTime(request.getEndTime())
                .guestCount(request.getGuestCount())
                .customerNote(request.getCustomerNote())
                .status(ReservationStatus.REQUESTED)
                .totalCharge(chargeBreakdown.getTotalCharge())
                .handlingFee(chargeBreakdown.getHandlingFee())
                .build();

        Reservation saved = reservationRepository.save(reservation);

        webSocketNotificationService.broadcastNewReservationRequest(branch.getId(), saved.getId());

        try {
            emailService.sendSimpleEmail(user.getEmail(), "Reservation Request Received",
                    "We have received your reservation request for " + branch.getName() + " on "
                            + saved.getReservationTime() + ". We will review it shortly.");
        } catch (Exception e) {
            // Ignore email errors if SMTP is down
        }

        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerReservationResponse> getMyReservations(String customerEmail, int page, int size, String tab) {
        User user = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Customer customer = customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        List<ReservationStatus> statuses;
        if ("upcoming".equalsIgnoreCase(tab)) {
            statuses = List.of(ReservationStatus.PAID);
        } else if ("history".equalsIgnoreCase(tab)) {
            statuses = List.of(ReservationStatus.COMPLETED, ReservationStatus.CANCELLED, ReservationStatus.REJECTED, ReservationStatus.EXPIRED);
        } else {
            // default to requests
            statuses = List.of(ReservationStatus.REQUESTED, ReservationStatus.CONFIRMED);
        }

        return reservationRepository
                .findByCustomerIdAndStatusInOrderByReservationTimeDesc(customer.getId(), statuses, PageRequest.of(page, size))
                .map(this::toDTO);
    }

    
    @Override
    public CustomerReservationResponse getReservationById(Long reservationId, String customerEmail) {
        Reservation r = getReservationWithAuthCheck(reservationId, customerEmail);
        return toDTO(r);
    }

    @Override
    @Transactional
    public void cancelMyReservation(Long reservationId, String reason, String customerEmail) {
        Reservation r = getReservationWithAuthCheck(reservationId, customerEmail);

        if (r.getStatus() == ReservationStatus.CANCELLED || r.getStatus() == ReservationStatus.EXPIRED
                || r.getStatus() == ReservationStatus.COMPLETED || r.getStatus() == ReservationStatus.REJECTED) {
            throw new RuntimeException("Reservation cannot be cancelled in its current state");
        }

        if (r.getStatus() == ReservationStatus.PAID) {
            // Process refund
            BigDecimal refundAmount = r.getTotalCharge().subtract(r.getHandlingFee());
            r.setRefundAmount(refundAmount);

            // Dummy refund record
            ReservationPayment refund = ReservationPayment.builder()
                    .reservation(r)
                    .paymentMethod(PaymentMethod.CARD)
                    .paymentStatus(PaymentStatus.SUCCESS)
                    .transactionReference("REFUND-" + System.currentTimeMillis())
                    .amount(refundAmount.negate())
                    .paidAt(LocalDateTime.now())
                    .build();
            reservationPaymentRepository.save(refund);

            // Deduct from total spent
            Customer c = r.getCustomer();
            c.setTotalSpent(c.getTotalSpent().subtract(refundAmount));
            customerRepository.save(c);

            try {
                emailService.sendSimpleEmail(c.getUser().getEmail(), "Reservation Cancelled & Refunded",
                        "Your reservation has been cancelled. An amount of " + refundAmount + " has been refunded.");
            } catch (Exception e) {
            }
        } else {
            try {
                emailService.sendSimpleEmail(r.getCustomer().getUser().getEmail(), "Reservation Cancelled",
                        "Your reservation has been cancelled as requested.");
            } catch (Exception e) {
            }
        }

        r.setStatus(ReservationStatus.CANCELLED);
        r.setCancelReason(reason);
        reservationRepository.save(r);

        // Free tables
        LocalDateTime now = LocalDateTime.now();
        boolean anyFreed = false;
        for (RestaurantTable table : r.getTables()) {
            if (table.getState() == TableStatus.RESERVED || table.getState() == TableStatus.OCCUPIED) {
                table.setState(TableStatus.AVAILABLE);
                table.setCurrentGuestCount(0);
                table.setStatusUpdatedAt(now);
                if (r.getId().equals(table.getSeatedReservationId())) {
                    table.setSeatedReservationId(null);
                }
                restaurantTableRepository.save(table);
                anyFreed = true;
            }
        }

        Long branchId = r.getBranch() != null ? r.getBranch().getId() : null;
        if (branchId != null) {
            if (anyFreed) webSocketNotificationService.broadcastTableUpdate(branchId);
            webSocketNotificationService.broadcastReservationUpdate(branchId);
        }
    }

    @Override
    @Transactional
    public CustomerReservationResponse payReservation(Long reservationId, String transactionRef, String customerEmail) {
        Reservation r = getReservationWithAuthCheck(reservationId, customerEmail);

        if (r.getStatus() != ReservationStatus.CONFIRMED) {
            throw new RuntimeException("Reservation is not ready for payment");
        }

        if (r.getPaymentDeadline() != null && r.getPaymentDeadline().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Payment window has expired");
        }

        ReservationPayment payment = ReservationPayment.builder()
                .reservation(r)
                .paymentMethod(PaymentMethod.CARD)
                .paymentStatus(PaymentStatus.PAID)
                .transactionReference(transactionRef)
                .amount(r.getTotalCharge())
                .paidAt(LocalDateTime.now())
                .build();
        reservationPaymentRepository.save(payment);

        r.setStatus(ReservationStatus.PAID);
        reservationRepository.save(r);

        Customer c = r.getCustomer();
        c.setTotalSpent(c.getTotalSpent().add(r.getTotalCharge()));
        customerRepository.save(c);

        Long branchId = r.getBranch() != null ? r.getBranch().getId() : null;
        if (branchId != null) {
            webSocketNotificationService.broadcastReservationUpdate(branchId);
        }

        try {
            emailService.sendSimpleEmail(c.getUser().getEmail(), "Reservation Confirmed",
                    "Your payment was successful and your reservation is now confirmed!");
        } catch (Exception e) {
        }

        return toDTO(r);
    }

    @Override
    public ReservationChargeBreakdown previewCharge(Long branchId, int guestCount, LocalDateTime start,
            LocalDateTime end) {
        BranchConfig config = branchConfigRepository.findByBranchId(branchId)
                .orElseThrow(() -> new RuntimeException("Branch config not found"));
        return calculateCharge(guestCount, start, end, config);
    }

    private ReservationChargeBreakdown calculateCharge(int guestCount, LocalDateTime start, LocalDateTime end,
            BranchConfig config) {
        long minutes = Duration.between(start, end).toMinutes();
        long hours = (long) Math.ceil(minutes / 60.0);

        BigDecimal feePerHour = config.getReservationFeePerHour();
        BigDecimal handlingFee = config.getReservationHandlingFee();

        // totalCharge = (reservationFeePerHour * ceil(durationInHours) * guestCount) +
        // reservationHandlingFee
        BigDecimal timeCharge = feePerHour.multiply(BigDecimal.valueOf(hours)).multiply(BigDecimal.valueOf(guestCount));
        BigDecimal total = timeCharge.add(handlingFee);

        return ReservationChargeBreakdown.builder()
                .timeCharge(timeCharge)
                .handlingFee(handlingFee)
                .totalCharge(total)
                .build();
    }

    private Reservation getReservationWithAuthCheck(Long reservationId, String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));
        if (r.getCustomer() == null || !r.getCustomer().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to access this reservation");
        }
        return r;
    }

    private CustomerReservationResponse toDTO(Reservation r) {
        List<Integer> tableNumbers = null;
        if (r.getTables() != null && !r.getTables().isEmpty()) {
            tableNumbers = r.getTables().stream()
                    .map(RestaurantTable::getTableNumber)
                    .sorted()
                    .toList();
        }

        return CustomerReservationResponse.builder()
                .id(r.getId())
                .branchName(r.getBranch() != null ? r.getBranch().getName() : null)
                .branchId(r.getBranch() != null ? r.getBranch().getId() : null)
                .latitude(r.getBranch() != null ? r.getBranch().getLatitude() : null)
                .longitude(r.getBranch() != null ? r.getBranch().getLongitude() : null)
                .startTime(r.getReservationTime())
                .endTime(r.getEndTime())
                .guestCount(r.getGuestCount())
                .customerNote(r.getCustomerNote())
                .receptionistNote(r.getReceptionistNote())
                .status(r.getStatus().name())
                .totalCharge(r.getTotalCharge())
                .handlingFee(r.getHandlingFee())
                .paymentDeadline(r.getPaymentDeadline())
                .tableNumbers(tableNumbers)
                .createdAt(r.getCreatedAt())
                .build();
    }

    @Override
    public List<com.ByteKnights.com.resturarent_system.dto.BranchResponse> getActiveReservationBranches() {
        return branchRepository.findByStatus(BranchStatus.ACTIVE).stream()
                .filter(branch -> {
                    BranchConfig config = branchConfigRepository.findByBranchId(branch.getId()).orElse(null);
                    return config != null && config.isBranchActiveForOrders() && config.isDineInEnabled()
                            && config.isReservationsEnabled();
                })
                .map(branch -> {
                    BranchConfig config = branchConfigRepository.findByBranchId(branch.getId()).orElse(null);
                    return com.ByteKnights.com.resturarent_system.dto.BranchResponse.builder()
                        .id(branch.getId())
                        .name(branch.getName())
                        .address(branch.getAddress())
                        .contactNumber(branch.getContactNumber())
                        .email(branch.getEmail())
                        .status(branch.getStatus().name())
                        .latitude(branch.getLatitude())
                        .longitude(branch.getLongitude())
                        .reservationMinLeadHours(config != null ? config.getReservationMinLeadHours() : null)
                        .reservationPaymentWindowMinutes(config != null ? config.getReservationPaymentWindowMinutes() : null)
                        .createdAt(branch.getCreatedAt())
                        .build();
                })
                .toList();
    }

}
