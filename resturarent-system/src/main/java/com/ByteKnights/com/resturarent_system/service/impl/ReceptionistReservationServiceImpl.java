package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.receptionist.CancelReservationRequest;
import com.ByteKnights.com.resturarent_system.dto.request.receptionist.CheckAvailabilityRequest;
import com.ByteKnights.com.resturarent_system.dto.request.receptionist.ConfirmReservationRequest;
import com.ByteKnights.com.resturarent_system.dto.request.receptionist.RejectReservationRequest;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.CheckAvailabilityResponse;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.PagedResponse;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReservationResponseDTO;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.TableAvailabilityDTO;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.*;
import com.ByteKnights.com.resturarent_system.service.AuditLogService;
import com.ByteKnights.com.resturarent_system.service.ReceptionistReservationService;
import com.ByteKnights.com.resturarent_system.service.WebSocketNotificationService;
import com.ByteKnights.com.resturarent_system.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import com.ByteKnights.com.resturarent_system.service.SystemConfigService;
import com.ByteKnights.com.resturarent_system.dto.cache.BranchConfigCacheDto;

/**
 * Reservation logic for the receptionist. Key operations:
 * - checkAvailability: tag every branch table FREE/OCCUPIED/BLOCKED for a slot
 * (only a real
 * time overlap BLOCKS; a within-1-hour gap only warns).
 * - confirmReservation / rejectReservation: act on a customer's REQUESTED
 * booking.
 * - seatReservation: occupy the booking's tables and mark it COMPLETED.
 * - cancelReservation: cancel + free tables (refund handled per the refund
 * rules).
 */
@Service
@RequiredArgsConstructor
public class ReceptionistReservationServiceImpl implements ReceptionistReservationService {

    private final ReservationRepository reservationRepository;
    private final RestaurantTableRepository tableRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final OrderRepository orderRepository;
    private final SystemConfigService systemConfigService;
    private final ReservationPaymentRepository reservationPaymentRepository;
    private final CustomerRepository customerRepository;
    private final WebSocketNotificationService webSocketNotificationService;
    private final EmailService emailService;
    private final com.ByteKnights.com.resturarent_system.service.StripePaymentService stripePaymentService;
    private final AuditLogService auditLogService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    // Tunable business rules
    private static final int MIN_RESERVATION_LEAD_HOURS = 0; // 0 = future-only (booking just has to be later than now)
    private static final int GAP_HOURS = 1; // required gap between two reservations on the same table

    @Override
    public CheckAvailabilityResponse checkAvailability(CheckAvailabilityRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));
        Long branchId = staff.getBranch().getId();

        LocalDateTime start = request.getReservationTime();
        LocalDateTime end = request.getEndTime();

        LocalDateTime earliestAllowed = LocalDateTime.now().plusHours(MIN_RESERVATION_LEAD_HOURS);

        // 1. Minimum lead-time gate
        if (start.isBefore(earliestAllowed)) {
            return CheckAvailabilityResponse.builder()
                    .possible(false)
                    .reason("Reservation time must be in the future.")
                    .earliestAllowed(earliestAllowed)
                    .tables(new ArrayList<>())
                    .build();
        }

        // basic sanity: end must be after start
        if (end == null || !end.isAfter(start)) {
            return CheckAvailabilityResponse.builder()
                    .possible(false)
                    .reason("End time must be after the start time.")
                    .tables(new ArrayList<>())
                    .build();
        }

        // Manual selection — no auto-allocation. Tag EVERY table in the branch for this
        // slot.
        List<RestaurantTable> tables = tableRepository.findByBranchId(branchId);
        LocalDate today = LocalDate.now();
        // Current occupancy only matters for a TODAY booking — for a future day whoever
        // is seated now is gone.
        boolean bookingIsToday = start.toLocalDate().equals(today);
        List<TableAvailabilityDTO> result = new ArrayList<>();
        for (RestaurantTable t : tables) {
            // Compare the requested slot against this table's PENDING reservations. Widen
            // by the gap so
            // near-but-not-overlapping bookings are found too (they only warn, they never
            // block).
            List<Reservation> nearby = reservationRepository.findOverlappingReservations(
                    t.getId(), start.minusHours(GAP_HOURS), end.plusHours(GAP_HOURS));
            TableAvailabilityDTO.TableAvailabilityDTOBuilder dto = TableAvailabilityDTO.builder()
                    .tableId(t.getId())
                    .tableNumber(t.getTableNumber())
                    .capacity(t.getCapacity());

            // A real time overlap is the ONLY thing that blocks the table.
            Reservation overlap = nearby.stream()
                    .filter(r -> r.getReservationTime().isBefore(end) && r.getEndTime().isAfter(start))
                    .findFirst().orElse(null);

            if (overlap != null) {
                // Time overlap → BLOCKED (removed from the picker).
                dto.status("BLOCKED")
                        .conflictStart(overlap.getReservationTime())
                        .conflictEnd(overlap.getEndTime())
                        .gapConflict(false);
            } else {
                // No overlap → selectable. Occupied-now details are shown ONLY for a TODAY
                // booking
                // (for a future day whoever is seated now will be long gone, so occupancy is
                // irrelevant).
                if (bookingIsToday && t.getState() == TableStatus.OCCUPIED) {
                    // Pending = orders not yet fully served (an order is "served" once all its
                    // items are).
                    long pendingOrders = orderRepository.findByTableIdAndStatusNotIn(
                            t.getId(), List.of(OrderStatus.CANCELLED, OrderStatus.REJECTED, OrderStatus.ON_HOLD))
                            .stream()
                            .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().toLocalDate().equals(today))
                            .filter(o -> o.getStatus() != OrderStatus.SERVED && o.getStatus() != OrderStatus.COMPLETED)
                            .count();
                    dto.status("OCCUPIED")
                            .occupiedSince(t.getStatusUpdatedAt())
                            .pendingOrderCount((int) pendingOrders);
                    if (t.getSeatedReservationId() != null) {
                        reservationRepository.findById(t.getSeatedReservationId()).ifPresent(sr -> {
                            dto.occupiedReservationStart(sr.getReservationTime());
                            dto.occupiedReservationEnd(sr.getEndTime());
                        });
                    }
                } else {
                    dto.status("FREE");
                }
                // Gap warning: a PENDING reservation within an hour (no overlap). Warn only —
                // never blocks.
                if (!nearby.isEmpty()) {
                    Reservation gapClash = nearby.get(0);
                    dto.gapConflict(true)
                            .conflictStart(gapClash.getReservationTime())
                            .conflictEnd(gapClash.getEndTime());
                }
            }
            result.add(dto.build());
        }

        // Only a time overlap (BLOCKED) removes a table; everything else is selectable.
        boolean possible = result.stream().anyMatch(d -> !"BLOCKED".equals(d.getStatus()));

        return CheckAvailabilityResponse.builder()
                .possible(possible)
                .reason(possible ? null : "Every table has a time conflict for this slot.")
                .tables(result)
                .build();
    }

    @Override
    public List<ReservationResponseDTO> getUpcomingReservations(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));

        Long branchId = staff.getBranch().getId();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneYearLater = now.plusYears(1);

        return reservationRepository.findByBranchAndDate(branchId, now, oneYearLater)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public ReservationResponseDTO getTableNextReservation(Long tableId, String userEmail) {
        return reservationRepository
                .findOverlappingReservations(tableId, LocalDateTime.now(), LocalDateTime.now().plusYears(1))
                .stream()
                .findFirst()
                .map(this::toDTO)
                .orElse(null);
    }

    @Override
    @Transactional
    public void cancelReservation(Long reservationId, CancelReservationRequest request) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        Long branchId = reservation.getBranch() != null ? reservation.getBranch().getId() : null;
        LocalDateTime now = LocalDateTime.now();

        // Terminal states can't be cancelled: already finished (seated), or already
        // ended.
        ReservationStatus current = reservation.getStatus();
        if (current == ReservationStatus.COMPLETED || current == ReservationStatus.CANCELLED
                || current == ReservationStatus.REJECTED || current == ReservationStatus.EXPIRED) {
            throw new RuntimeException("This reservation can no longer be cancelled");
        }

        Map<String, Object> oldValues = buildReservationAuditSnapshot(reservation);
        oldValues.put("payments", buildReservationPaymentAuditSnapshots(reservation.getId()));
        oldValues.put("customer", buildCustomerAuditSnapshot(reservation.getCustomer()));

        // ── Refund decision (receptionist-initiated cancel)
        // ──────────────────────────────
        // Only a PAID booking has money. When the RESTAURANT cancels BEFORE the booking
        // starts,
        // the customer is fully refunded (including the handling fee). After it has
        // started
        // (e.g. a no-show the receptionist finally cancels) nothing is refunded.
        // Not-yet-paid
        // bookings (REQUESTED/CONFIRMED) never had money, so no refund either.
        BigDecimal refundAmount = null;
        boolean isRefunded = false;
        if (current == ReservationStatus.PAID
                && reservation.getReservationTime() != null
                && now.isBefore(reservation.getReservationTime())) {
            refundAmount = reservation.getTotalCharge();
        }

        if (refundAmount != null && refundAmount.signum() > 0) {
            reservation.setRefundAmount(refundAmount);

            ReservationPayment originalPayment = reservationPaymentRepository.findByReservationIdOrderByIdAsc(reservation.getId())
                    .stream()
                    .filter(p -> p.getPaymentStatus() == com.ByteKnights.com.resturarent_system.entity.PaymentStatus.PAID
                              || p.getPaymentStatus() == com.ByteKnights.com.resturarent_system.entity.PaymentStatus.SUCCESS)
                    .findFirst()
                    .orElse(null);
            String originalTransactionRef = "UNKNOWN";

            if (originalPayment != null && originalPayment.getTransactionReference() != null) {
                originalTransactionRef = originalPayment.getTransactionReference();

                String idempotencyKey = "recept-res-cancel-" + reservation.getId();
                Map<String, String> metadata = new HashMap<>();
                metadata.put("reservationId", String.valueOf(reservation.getId()));
                metadata.put("cancelReason", request.getReason());

                boolean refundSuccess = stripePaymentService.refundPayment(
                        originalTransactionRef,
                        refundAmount,
                        idempotencyKey,
                        "requested_by_customer",
                        metadata);

                if (!refundSuccess) {
                    originalPayment.setPaymentStatus(PaymentStatus.REFUND_FAILED);
                } else {
                    originalPayment.setPaymentStatus(PaymentStatus.REFUNDED);
                    isRefunded = true;
                }
                reservationPaymentRepository.save(originalPayment);
            }

            // Record the refund as a negative payment against this reservation.
            ReservationPayment refund = ReservationPayment.builder()
                    .reservation(reservation)
                    .paymentMethod(PaymentMethod.CARD)
                    .paymentStatus(isRefunded ? PaymentStatus.REFUNDED : PaymentStatus.REFUND_FAILED)
                    .transactionReference("REFUND-" + originalTransactionRef)
                    .amount(refundAmount.negate())
                    .refundAmount(refundAmount)
                    .refundedAt(now)
                    .build();
            reservationPaymentRepository.save(refund);

            // Take the refunded amount back out of the customer's lifetime spend.
            Customer c = reservation.getCustomer();
            if (isRefunded && c != null && c.getTotalSpent() != null) {
                c.setTotalSpent(c.getTotalSpent().subtract(refundAmount));
                customerRepository.save(c);
            }
        }

        // One reservation = one booking, so this cancels the whole booking (all its
        // tables) at once.
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelReason(request.getReason());
        reservationRepository.save(reservation);

        // Free each table this booking was holding. A RESERVED table is released unless
        // another
        // CONFIRMED/PAID reservation still holds it within the 15-minute window.
        boolean anyFreed = false;
        for (RestaurantTable table : reservation.getTables()) {
            // Clear any lingering seated link to this booking (safety — a cancellable
            // booking
            // shouldn't be seated, but never leave a stale pointer).
            if (reservation.getId().equals(table.getSeatedReservationId())) {
                table.setSeatedReservationId(null);
            }
            if (table.getState() == TableStatus.RESERVED) {
                boolean stillHeld = !reservationRepository
                        .findOverlappingReservations(table.getId(), now, now.plusMinutes(15))
                        .isEmpty();
                if (!stillHeld) {
                    table.setState(TableStatus.AVAILABLE);
                    table.setCurrentGuestCount(0);
                    table.setStatusUpdatedAt(now);
                    tableRepository.save(table);
                    anyFreed = true;
                }
            }
        }

        Map<String, Object> newValues = buildReservationAuditSnapshot(reservation);
        newValues.put("payments", buildReservationPaymentAuditSnapshots(reservation.getId()));
        newValues.put("customer", buildCustomerAuditSnapshot(reservation.getCustomer()));
        newValues.put("refundAmount", refundAmount);
        newValues.put("refundProcessed", isRefunded);
        newValues.put("tablesFreed", anyFreed);

        auditLogService.logCurrentUserAction(
                AuditModule.RESERVATION,
                AuditEventType.RESERVATION_CANCELLED,
                AuditStatus.SUCCESS,
                AuditSeverity.WARN,
                AuditTargetType.RESERVATION,
                reservation.getId(),
                branchId,
                "Reservation cancelled by receptionist",
                oldValues,
                newValues
        );

        // Tell the customer (WS + email), and refresh the branch views — deferred until
        // commit so
        // the pushed update is never ahead of what a follow-up read can actually see.
        boolean finalAnyFreed = anyFreed;
        BigDecimal finalRefundAmount = refundAmount;
        final boolean finalIsRefunded = isRefunded;
        runAfterCommit(() -> {
            if (reservation.getCustomer() != null && reservation.getCustomer().getUser() != null) {
                webSocketNotificationService.broadcastReservationStatusToCustomer(
                        reservation.getCustomer().getUser().getId(), reservation.getId(), "CANCELLED");
                try {
                    String refundMsg = "";
                    if (finalRefundAmount != null && finalRefundAmount.signum() > 0) {
                        refundMsg = finalIsRefunded 
                                ? " A refund of Rs " + finalRefundAmount + " has been processed."
                                : " However, the automatic refund failed. Our staff will process it manually.";
                    }
                    emailService.sendSimpleEmail(reservation.getCustomer().getUser().getEmail(),
                            "Reservation Cancelled",
                            "Your reservation at " + reservation.getBranch().getName()
                                    + " has been cancelled. Reason: " + request.getReason() + "." + refundMsg);
                } catch (Exception ignored) {
                }
            }

            if (branchId != null) {
                if (finalAnyFreed)
                    webSocketNotificationService.broadcastTableUpdate(branchId);
                webSocketNotificationService.broadcastReservationUpdate(branchId);
            }
        });
    }

    @Override
    @Transactional
    public void seatReservation(Long reservationId, Integer guestCount, String userEmail) {
        Long branchId = getBranchId(userEmail);

        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        if (r.getBranch() == null || !r.getBranch().getId().equals(branchId)) {
            throw new RuntimeException("Reservation does not belong to your branch");
        }
        if (r.getStatus() != ReservationStatus.PAID) {
            throw new RuntimeException("This reservation is not active or paid");
        }

        Map<String, Object> oldValues = buildReservationAuditSnapshot(r);
        oldValues.put("payments", buildReservationPaymentAuditSnapshots(r.getId()));
        oldValues.put("customer", buildCustomerAuditSnapshot(r.getCustomer()));

        // Seat the whole party: occupy every table of the booking. The guest count is
        // distributed
        // greedily across the tables (each filled up to its seats), and the booking is
        // marked completed.
        int remaining = guestCount != null ? guestCount
                : (r.getGuestCount() != null ? r.getGuestCount() : 0);
        LocalDateTime now = LocalDateTime.now();
        List<RestaurantTable> orderedTables = r.getTables().stream()
                .sorted(Comparator.comparingInt(t -> t.getCapacity() != null ? t.getCapacity() : 0))
                .toList();
        for (RestaurantTable table : orderedTables) {
            int cap = table.getCapacity() != null ? table.getCapacity() : 0;
            int seat = Math.min(Math.max(remaining, 0), cap);
            remaining -= seat;
            table.setState(TableStatus.OCCUPIED);
            table.setCurrentGuestCount(seat);
            table.setSeatedReservationId(r.getId());
            table.setStatusUpdatedAt(now);
            tableRepository.save(table);
        }

        r.setStatus(ReservationStatus.COMPLETED);
        reservationRepository.save(r);

        Map<String, Object> newValues = buildReservationAuditSnapshot(r);
        newValues.put("payments", buildReservationPaymentAuditSnapshots(r.getId()));
        newValues.put("customer", buildCustomerAuditSnapshot(r.getCustomer()));
        newValues.put("seatedGuestCount", guestCount);

        auditLogService.logCurrentUserAction(
                AuditModule.RESERVATION,
                AuditEventType.RESERVATION_SEATED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.RESERVATION,
                r.getId(),
                branchId,
                "Reservation seated by receptionist",
                oldValues,
                newValues
        );

        runAfterCommit(() -> {
            webSocketNotificationService.broadcastTableUpdate(branchId);
            webSocketNotificationService.broadcastReservationUpdate(branchId);

            if (r.getCustomer() != null && r.getCustomer().getUser() != null) {
                try {
                    emailService.sendSimpleEmail(r.getCustomer().getUser().getEmail(), "Thank You for Dining With Us",
                            "Thank you for dining at " + r.getBranch().getName() + " today. "
                                    + "We hope to see you again soon!");
                } catch (Exception ignored) {
                }
            }
        });
    }

    @Override
    public PagedResponse<ReservationResponseDTO> getAllReservations(
            String userEmail, int page, int size, String date, Integer tableNumber, String status) {
        Long branchId = getBranchId(userEmail);

        // Optional day filter: a single calendar day → [startOfDay, startOfNextDay)
        LocalDateTime dayStart = null;
        LocalDateTime dayEnd = null;
        if (date != null && !date.isBlank()) {
            LocalDate day = LocalDate.parse(date); // expects yyyy-MM-dd
            dayStart = day.atStartOfDay();
            dayEnd = dayStart.plusDays(1);
        }

        // Optional status filter
        ReservationStatus statusFilter = null;
        if (status != null && !status.isBlank()) {
            statusFilter = ReservationStatus.valueOf(status.toUpperCase());
        }

        // Ordering ("upcoming first, then past") is defined in the repository query
        // itself.
        Pageable pageable = PageRequest.of(page, size);
        Page<Reservation> result = reservationRepository.findFilteredByBranch(
                branchId, tableNumber, statusFilter, dayStart, dayEnd, LocalDateTime.now(), pageable);

        return PagedResponse.<ReservationResponseDTO>builder()
                .content(result.getContent().stream().map(this::toDTO).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Override
    @Transactional
    public void confirmReservation(Long reservationId, ConfirmReservationRequest request, String userEmail) {
        Long branchId = getBranchId(userEmail);

        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        if (r.getBranch() == null || !r.getBranch().getId().equals(branchId)) {
            throw new RuntimeException("Reservation does not belong to your branch");
        }
        if (r.getStatus() != ReservationStatus.REQUESTED) {
            throw new RuntimeException("Only a requested reservation can be confirmed");
        }

        BranchConfigCacheDto config = systemConfigService.getCachedBranchConfig(branchId);

        // Load the assigned tables and validate they all belong to this branch.
        List<RestaurantTable> assignedTables = tableRepository
                .findByBranchIdAndTableNumberIn(branchId, request.getTableNumbers());
        if (assignedTables.size() != request.getTableNumbers().size()) {
            throw new RuntimeException("One or more selected tables were not found in your branch");
        }

        // A real time overlap (with a DIFFERENT reservation) on any assigned table
        // blocks confirmation.
        for (RestaurantTable t : assignedTables) {
            boolean clash = reservationRepository
                    .findOverlappingReservations(t.getId(), r.getReservationTime(), r.getEndTime())
                    .stream().anyMatch(o -> !o.getId().equals(r.getId()));
            if (clash) {
                throw new RuntimeException("Table " + t.getTableNumber()
                        + " already has a reservation overlapping this slot");
            }
        }

        Map<String, Object> oldValues = buildReservationAuditSnapshot(r);
        oldValues.put("payments", buildReservationPaymentAuditSnapshots(r.getId()));
        oldValues.put("customer", buildCustomerAuditSnapshot(r.getCustomer()));

        // Assign the tables to this booking (writes the reservation_tables rows).
        r.setTables(new HashSet<>(assignedTables));

        // Lock any AVAILABLE assigned table whose slot starts within 15 min → RESERVED
        // now.
        LocalDateTime now = LocalDateTime.now();
        boolean anyLocked = false;
        for (RestaurantTable t : assignedTables) {
            if (t.getState() == TableStatus.AVAILABLE && !r.getReservationTime().isAfter(now.plusMinutes(15))) {
                t.setState(TableStatus.RESERVED);
                t.setStatusUpdatedAt(now);
                tableRepository.save(t);
                anyLocked = true;
            }
        }

        if (request.getNote() != null && !request.getNote().isBlank()) {
            r.setReceptionistNote(request.getNote());
        }

        r.setStatus(ReservationStatus.CONFIRMED);
        r.setPaymentDeadline(now.plusMinutes(config.getReservationPaymentWindowMinutes()));
        reservationRepository.save(r);

        Map<String, Object> newValues = buildReservationAuditSnapshot(r);
        newValues.put("payments", buildReservationPaymentAuditSnapshots(r.getId()));
        newValues.put("customer", buildCustomerAuditSnapshot(r.getCustomer()));
        newValues.put("assignedTableNumbers", request.getTableNumbers());
        newValues.put("paymentWindowMinutes", config.getReservationPaymentWindowMinutes());
        newValues.put("tablesLockedImmediately", anyLocked);

        auditLogService.logCurrentUserAction(
                AuditModule.RESERVATION,
                AuditEventType.RESERVATION_CONFIRMED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.RESERVATION,
                r.getId(),
                branchId,
                "Reservation confirmed by receptionist",
                oldValues,
                newValues
        );

        // Notify the customer (WS + email with the pay link) and refresh branch views —
        // deferred
        // until commit so the push never arrives before the CONFIRMED row is readable.
        boolean finalAnyLocked = anyLocked;
        runAfterCommit(() -> {
            if (r.getCustomer() != null && r.getCustomer().getUser() != null) {
                webSocketNotificationService.broadcastReservationStatusToCustomer(
                        r.getCustomer().getUser().getId(), r.getId(), "CONFIRMED");
                try {
                    int window = config.getReservationPaymentWindowMinutes();
                    emailService.sendSimpleEmail(r.getCustomer().getUser().getEmail(), "Reservation Confirmed",
                            "Your reservation at " + r.getBranch().getName() + " is confirmed. "
                                    + "Please complete payment within " + window + " minutes to secure it: "
                                    + frontendUrl + "/reservations");
                } catch (Exception ignored) {
                }
            }
            if (finalAnyLocked)
                webSocketNotificationService.broadcastTableUpdate(branchId);
            webSocketNotificationService.broadcastReservationUpdate(branchId);
        });
    }

    @Override
    @Transactional
    public void rejectReservation(Long reservationId, RejectReservationRequest request, String userEmail) {
        Long branchId = getBranchId(userEmail);

        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        if (r.getBranch() == null || !r.getBranch().getId().equals(branchId)) {
            throw new RuntimeException("Reservation does not belong to your branch");
        }
        if (r.getStatus() != ReservationStatus.REQUESTED) {
            throw new RuntimeException("Only a requested reservation can be rejected");
        }

        Map<String, Object> oldValues = buildReservationAuditSnapshot(r);
        oldValues.put("payments", buildReservationPaymentAuditSnapshots(r.getId()));
        oldValues.put("customer", buildCustomerAuditSnapshot(r.getCustomer()));

        r.setStatus(ReservationStatus.REJECTED);
        r.setReceptionistNote(request.getReason());
        reservationRepository.save(r);

        Map<String, Object> newValues = buildReservationAuditSnapshot(r);
        newValues.put("payments", buildReservationPaymentAuditSnapshots(r.getId()));
        newValues.put("customer", buildCustomerAuditSnapshot(r.getCustomer()));
        newValues.put("rejectionReason", request.getReason());

        auditLogService.logCurrentUserAction(
                AuditModule.RESERVATION,
                AuditEventType.RESERVATION_REJECTED,
                AuditStatus.SUCCESS,
                AuditSeverity.WARN,
                AuditTargetType.RESERVATION,
                r.getId(),
                branchId,
                "Reservation rejected by receptionist",
                oldValues,
                newValues
        );

        runAfterCommit(() -> {
            if (r.getCustomer() != null && r.getCustomer().getUser() != null) {
                webSocketNotificationService.broadcastReservationStatusToCustomer(
                        r.getCustomer().getUser().getId(), r.getId(), "REJECTED");
                try {
                    emailService.sendSimpleEmail(r.getCustomer().getUser().getEmail(), "Reservation Rejected",
                            "Unfortunately your reservation request for " + r.getBranch().getName()
                                    + " could not be accommodated. Reason: " + request.getReason());
                } catch (Exception ignored) {
                }
            }
            webSocketNotificationService.broadcastReservationUpdate(branchId);
        });
    }

    @Override
    public List<ReservationResponseDTO> getRequestedReservations(String userEmail) {
        Long branchId = getBranchId(userEmail);
        return reservationRepository
                .findByBranchIdAndStatusOrderByCreatedAtAsc(branchId, ReservationStatus.REQUESTED)
                .stream().map(this::toDTO).toList();
    }

    @Override
    public List<ReservationResponseDTO> getUpcomingQueue(String userEmail) {
        Long branchId = getBranchId(userEmail);
        return reservationRepository
                .findByBranchIdAndStatusInOrderByReservationTimeAsc(branchId,
                        List.of(ReservationStatus.CONFIRMED, ReservationStatus.PAID))
                .stream().map(this::toDTO).toList();
    }


    // ─────────────────────────────────────────────────────────────────────────
    // AUDIT HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private Map<String, Object> buildReservationAuditSnapshot(Reservation reservation) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        if (reservation == null) {
            return snapshot;
        }

        snapshot.put("reservationId", reservation.getId());
        snapshot.put("status", reservation.getStatus() != null ? reservation.getStatus().name() : null);
        snapshot.put("branchId", reservation.getBranch() != null ? reservation.getBranch().getId() : null);
        snapshot.put("branchName", reservation.getBranch() != null ? reservation.getBranch().getName() : null);

        snapshot.put("customerName", reservation.getCustomerName());
        snapshot.put("customerPhone", reservation.getCustomerPhone());
        snapshot.put("reservationTime", reservation.getReservationTime());
        snapshot.put("endTime", reservation.getEndTime());
        snapshot.put("guestCount", reservation.getGuestCount());
        snapshot.put("customerNote", reservation.getCustomerNote());
        snapshot.put("receptionistNote", reservation.getReceptionistNote());
        snapshot.put("cancelReason", reservation.getCancelReason());
        snapshot.put("paymentDeadline", reservation.getPaymentDeadline());
        snapshot.put("totalCharge", reservation.getTotalCharge());
        snapshot.put("refundAmount", reservation.getRefundAmount());
        snapshot.put("createdAt", reservation.getCreatedAt());

        List<Map<String, Object>> tableSnapshots = reservation.getTables() != null
                ? reservation.getTables().stream()
                        .sorted(Comparator.comparingInt(t -> t.getTableNumber() != null ? t.getTableNumber() : 0))
                        .map(this::buildTableAuditSnapshot)
                        .toList()
                : List.of();

        snapshot.put("tables", tableSnapshots);

        return snapshot;
    }

    private Map<String, Object> buildTableAuditSnapshot(RestaurantTable table) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        if (table == null) {
            return snapshot;
        }

        snapshot.put("tableId", table.getId());
        snapshot.put("tableNumber", table.getTableNumber());
        snapshot.put("capacity", table.getCapacity());
        snapshot.put("state", table.getState() != null ? table.getState().name() : null);
        snapshot.put("currentGuestCount", table.getCurrentGuestCount());
        snapshot.put("seatedReservationId", table.getSeatedReservationId());
        snapshot.put("statusUpdatedAt", table.getStatusUpdatedAt());

        return snapshot;
    }

    private List<Map<String, Object>> buildReservationPaymentAuditSnapshots(Long reservationId) {
        if (reservationId == null) {
            return List.of();
        }

        return reservationPaymentRepository.findByReservationIdOrderByIdAsc(reservationId)
                .stream()
                .map(this::buildReservationPaymentAuditSnapshot)
                .toList();
    }

    private Map<String, Object> buildReservationPaymentAuditSnapshot(ReservationPayment payment) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        if (payment == null) {
            return snapshot;
        }

        snapshot.put("paymentId", payment.getId());
        snapshot.put("reservationId", payment.getReservation() != null ? payment.getReservation().getId() : null);
        snapshot.put("paymentMethod", payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : null);
        snapshot.put("paymentStatus", payment.getPaymentStatus() != null ? payment.getPaymentStatus().name() : null);
        snapshot.put("transactionReference", payment.getTransactionReference());
        snapshot.put("amount", payment.getAmount());
        snapshot.put("refundAmount", payment.getRefundAmount());
        snapshot.put("paidAt", payment.getPaidAt());
        snapshot.put("refundedAt", payment.getRefundedAt());

        return snapshot;
    }

    private Map<String, Object> buildCustomerAuditSnapshot(Customer customer) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        if (customer == null) {
            return snapshot;
        }

        User user = customer.getUser();

        snapshot.put("customerId", customer.getId());
        snapshot.put("userId", user != null ? user.getId() : null);
        snapshot.put("email", user != null ? user.getEmail() : null);
        snapshot.put("fullName", user != null ? user.getFullName() : null);
        snapshot.put("totalSpent", customer.getTotalSpent());

        return snapshot;
    }

    // Defers a WS broadcast / email until the enclosing transaction actually
    // commits, so the
    // client never receives a "reservation changed" push before the row is visible
    // to a follow-up
    // read (avoids a stale re-fetch race). Runs immediately if there's no active
    // transaction.
    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    private Long getBranchId(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));
        return staff.getBranch().getId();
    }

    // reservation.customer_name is just a snapshot taken at request time and is blank for
    // customers whose users.full_name was never set. Always prefer the live customer_id ->
    // customers -> users join so a later profile update shows up here too; fall back to the
    // snapshot only for legacy rows with no linked customer.
    private String resolveCustomerName(Reservation r) {
        if (r.getCustomer() != null && r.getCustomer().getUser() != null) {
            User u = r.getCustomer().getUser();
            if (u.getFullName() != null && !u.getFullName().isBlank()) {
                return u.getFullName();
            }
            if (u.getUsername() != null && !u.getUsername().isBlank()) {
                return u.getUsername();
            }
        }
        return r.getCustomerName();
    }

    private ReservationResponseDTO toDTO(Reservation r) {
        List<RestaurantTable> ts = r.getTables().stream()
                .sorted(Comparator.comparingInt(t -> t.getTableNumber() != null ? t.getTableNumber() : 0))
                .toList();

        ReservationResponseDTO.ReservationResponseDTOBuilder dto = ReservationResponseDTO.builder()
                .id(r.getId())
                .tableIds(ts.stream().map(RestaurantTable::getId).toList())
                .tableNumbers(ts.stream().map(RestaurantTable::getTableNumber).toList())
                .customerName(resolveCustomerName(r))
                .customerPhone(r.getCustomerPhone())
                .reservationTime(r.getReservationTime())
                .endTime(r.getEndTime())
                .guestCount(r.getGuestCount())
                .notes(r.getCustomerNote())
                .status(r.getStatus().name())
                .createdAt(r.getCreatedAt())
                .totalCharge(r.getTotalCharge())
                // CANCELLED stores its reason in cancelReason; REJECTED reuses receptionistNote
                // (set by rejectReservation() — never conflicts with the confirm-note use of that
                // same field, since a REQUESTED booking can only ever be confirmed OR rejected).
                .cancelReason(r.getStatus() == ReservationStatus.CANCELLED ? r.getCancelReason()
                        : r.getStatus() == ReservationStatus.REJECTED ? r.getReceptionistNote() : null);

        // Payment/refund rows only ever exist once money has moved — skip the extra
        // query for
        // REQUESTED/CONFIRMED/REJECTED/EXPIRED bookings that never reached a payment.
        if (r.getStatus() == ReservationStatus.PAID || r.getStatus() == ReservationStatus.COMPLETED
                || r.getStatus() == ReservationStatus.CANCELLED) {
            List<ReservationPayment> payments = reservationPaymentRepository.findByReservationIdOrderByIdAsc(r.getId());

            // A positive amount row = the original payment.
            payments.stream()
                    .filter(p -> p.getAmount() != null && p.getAmount().signum() > 0)
                    .findFirst()
                    .ifPresent(p -> dto.amountPaid(p.getAmount())
                            .paidAt(p.getPaidAt())
                            .paymentMethod(p.getPaymentMethod() != null ? p.getPaymentMethod().name() : null)
                            .transactionReference(p.getTransactionReference()));

            // A negative amount row = a refund. Some callers record the refund time in
            // paidAt
            // instead of refundedAt, so fall back between the two.
            payments.stream()
                    .filter(p -> p.getAmount() != null && p.getAmount().signum() < 0)
                    .findFirst()
                    .ifPresent(p -> dto
                            .refundAmount(p.getRefundAmount() != null ? p.getRefundAmount() : p.getAmount().negate())
                            .refundedAt(p.getRefundedAt() != null ? p.getRefundedAt() : p.getPaidAt())
                            .refundTransactionReference(p.getTransactionReference()));
        }

        return dto.build();
    }
}