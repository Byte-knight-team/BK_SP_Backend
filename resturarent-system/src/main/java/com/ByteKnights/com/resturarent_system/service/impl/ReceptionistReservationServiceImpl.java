package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.receptionist.CancelReservationRequest;
import com.ByteKnights.com.resturarent_system.dto.request.receptionist.CheckAvailabilityRequest;
import com.ByteKnights.com.resturarent_system.dto.request.receptionist.ConfirmReservationRequest;
import com.ByteKnights.com.resturarent_system.dto.request.receptionist.CreateReservationRequest;
import com.ByteKnights.com.resturarent_system.dto.request.receptionist.RejectReservationRequest;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.CheckAvailabilityResponse;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.PagedResponse;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReservationResponseDTO;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.TableAvailabilityDTO;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.*;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/**
 * Reservation logic for the receptionist. Key operations:
 *  - checkAvailability: tag every branch table FREE/OCCUPIED/BLOCKED for a slot (only a real
 *    time overlap BLOCKS; a within-1-hour gap only warns).
 *  - confirmReservation / rejectReservation: act on a customer's REQUESTED booking.
 *  - seatReservation: occupy the booking's tables and mark it COMPLETED.
 *  - cancelReservation: cancel + free tables (refund handled per the refund rules).
 */
@Service
@RequiredArgsConstructor
public class ReceptionistReservationServiceImpl implements ReceptionistReservationService {

    private final ReservationRepository reservationRepository;
    private final RestaurantTableRepository tableRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final OrderRepository orderRepository;
    private final BranchConfigRepository branchConfigRepository;
    private final WebSocketNotificationService webSocketNotificationService;
    private final EmailService emailService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    // Tunable business rules
    private static final int MIN_RESERVATION_LEAD_HOURS = 0; // 0 = future-only (booking just has to be later than now)
    private static final int GAP_HOURS = 1;                  // required gap between two reservations on the same table

    @Override
    @Transactional
    public ReservationResponseDTO createReservation(CreateReservationRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));

        Branch branch = staff.getBranch();
        Long branchId = branch.getId();

        // Load and validate the selected tables — all must exist and belong to this branch.
        List<RestaurantTable> tables = tableRepository.findAllById(request.getTableIds());
        if (tables.size() != request.getTableIds().size()) {
            throw new RuntimeException("One or more selected tables were not found");
        }
        for (RestaurantTable t : tables) {
            if (t.getBranch() == null || !t.getBranch().getId().equals(branchId)) {
                throw new RuntimeException("A selected table does not belong to your branch");
            }
        }

        // Basic sanity
        if (request.getReservationTime().isBefore(LocalDateTime.now().plusHours(MIN_RESERVATION_LEAD_HOURS))) {
            throw new RuntimeException("Reservation time must be in the future.");
        }
        if (request.getEndTime() == null || !request.getEndTime().isAfter(request.getReservationTime())) {
            throw new RuntimeException("End time must be after the start time.");
        }

        // Enforce capacity: the selected tables' seats must cover the party.
        int totalSeats = tables.stream().mapToInt(t -> t.getCapacity() != null ? t.getCapacity() : 0).sum();
        if (request.getGuestCount() > totalSeats) {
            throw new RuntimeException("Selected tables seat " + totalSeats
                    + ", which can't cover a party of " + request.getGuestCount() + ".");
        }

        // Block ONLY on a real time overlap with a PENDING reservation. The 1-hour gap is a warning
        // shown at check time — it never blocks; the receptionist decides.
        for (RestaurantTable t : tables) {
            List<Reservation> overlapping = reservationRepository.findOverlappingReservations(
                    t.getId(),
                    request.getReservationTime(),
                    request.getEndTime());
            if (!overlapping.isEmpty()) {
                throw new RuntimeException("Table " + t.getTableNumber()
                        + " already has a reservation overlapping this time slot");
            }
        }

        Reservation reservation = Reservation.builder()
                .branch(branch)
                .tables(new HashSet<>(tables))
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .reservationTime(request.getReservationTime())
                .endTime(request.getEndTime())
                .guestCount(request.getGuestCount())
                .customerNote(request.getNotes())
                .status(ReservationStatus.PAID)
                .build();

        Reservation saved = reservationRepository.save(reservation);

        // Immediate lock: any AVAILABLE selected table whose slot starts within 15 min → RESERVED now
        // (the scheduler's 15-min window would otherwise miss a booking made <15 min ahead).
        LocalDateTime now = LocalDateTime.now();
        boolean anyLocked = false;
        for (RestaurantTable t : tables) {
            if (t.getState() == TableStatus.AVAILABLE
                    && !saved.getReservationTime().isAfter(now.plusMinutes(15))) {
                t.setState(TableStatus.RESERVED);
                t.setStatusUpdatedAt(now);
                tableRepository.save(t);
                anyLocked = true;
            }
        }

        webSocketNotificationService.broadcastReservationUpdate(branchId);
        if (anyLocked) webSocketNotificationService.broadcastTableUpdate(branchId);

        return toDTO(saved);
    }

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

        // Manual selection — no auto-allocation. Tag EVERY table in the branch for this slot.
        List<RestaurantTable> tables = tableRepository.findByBranchId(branchId);
        LocalDate today = LocalDate.now();
        // Current occupancy only matters for a TODAY booking — for a future day whoever is seated now is gone.
        boolean bookingIsToday = start.toLocalDate().equals(today);
        List<TableAvailabilityDTO> result = new ArrayList<>();
        for (RestaurantTable t : tables) {
            // Compare the requested slot against this table's PENDING reservations. Widen by the gap so
            // near-but-not-overlapping bookings are found too (they only warn, they never block).
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
                // No overlap → selectable. Occupied-now details are shown ONLY for a TODAY booking
                // (for a future day whoever is seated now will be long gone, so occupancy is irrelevant).
                if (bookingIsToday && t.getState() == TableStatus.OCCUPIED) {
                    // Pending = orders not yet fully served (an order is "served" once all its items are).
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
                // Gap warning: a PENDING reservation within an hour (no overlap). Warn only — never blocks.
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
        return reservationRepository.findOverlappingReservations(tableId, LocalDateTime.now(), LocalDateTime.now().plusYears(1))
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

        // One reservation = one booking, so this cancels the whole booking (all its tables) at once.
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelReason(request.getReason());
        reservationRepository.save(reservation);

        // Free each table this booking was holding — for a RESERVED table, unless another PENDING
        // reservation still holds it within the 15-minute window (then keep it held for that one).
        LocalDateTime now = LocalDateTime.now();
        boolean anyFreed = false;
        for (RestaurantTable table : reservation.getTables()) {
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

        if (branchId != null) {
            if (anyFreed) webSocketNotificationService.broadcastTableUpdate(branchId);
            webSocketNotificationService.broadcastReservationUpdate(branchId);
        }
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

        // Seat the whole party: occupy every table of the booking. The guest count is distributed
        // greedily across the tables (each filled up to its seats), and the booking is marked completed.
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

        webSocketNotificationService.broadcastTableUpdate(branchId);
        webSocketNotificationService.broadcastReservationUpdate(branchId);
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

        // Ordering ("upcoming first, then past") is defined in the repository query itself.
        Pageable pageable = PageRequest.of(page, size);
        Page<Reservation> result = reservationRepository.findFilteredByBranch(
                branchId, tableNumber, statusFilter, dayStart, dayEnd, pageable);

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

        BranchConfig config = branchConfigRepository.findByBranchId(branchId)
                .orElseThrow(() -> new RuntimeException("Branch config not found"));

        // Load the assigned tables and validate they all belong to this branch.
        List<RestaurantTable> assignedTables = tableRepository
                .findByBranchIdAndTableNumberIn(branchId, request.getTableNumbers());
        if (assignedTables.size() != request.getTableNumbers().size()) {
            throw new RuntimeException("One or more selected tables were not found in your branch");
        }

        // A real time overlap (with a DIFFERENT reservation) on any assigned table blocks confirmation.
        for (RestaurantTable t : assignedTables) {
            boolean clash = reservationRepository
                    .findOverlappingReservations(t.getId(), r.getReservationTime(), r.getEndTime())
                    .stream().anyMatch(o -> !o.getId().equals(r.getId()));
            if (clash) {
                throw new RuntimeException("Table " + t.getTableNumber()
                        + " already has a reservation overlapping this slot");
            }
        }

        // Assign the tables to this booking (writes the reservation_tables rows).
        r.setTables(new HashSet<>(assignedTables));

        // Lock any AVAILABLE assigned table whose slot starts within 15 min → RESERVED now.
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

        // Notify the customer (WS + email with the pay link) and refresh branch views.
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
        if (anyLocked) webSocketNotificationService.broadcastTableUpdate(branchId);
        webSocketNotificationService.broadcastReservationUpdate(branchId);
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

        r.setStatus(ReservationStatus.REJECTED);
        r.setReceptionistNote(request.getReason());
        reservationRepository.save(r);

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
    }

    private Long getBranchId(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));
        return staff.getBranch().getId();
    }

    private ReservationResponseDTO toDTO(Reservation r) {
        List<RestaurantTable> ts = r.getTables().stream()
                .sorted(Comparator.comparingInt(t -> t.getTableNumber() != null ? t.getTableNumber() : 0))
                .toList();
        return ReservationResponseDTO.builder()
                .id(r.getId())
                .tableIds(ts.stream().map(RestaurantTable::getId).toList())
                .tableNumbers(ts.stream().map(RestaurantTable::getTableNumber).toList())
                .customerName(r.getCustomerName())
                .customerPhone(r.getCustomerPhone())
                .reservationTime(r.getReservationTime())
                .endTime(r.getEndTime())
                .guestCount(r.getGuestCount())
                .notes(r.getCustomerNote())
                .status(r.getStatus().name())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
