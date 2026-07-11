package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.receptionist.CancelReservationRequest;
import com.ByteKnights.com.resturarent_system.dto.request.receptionist.CheckAvailabilityRequest;
import com.ByteKnights.com.resturarent_system.dto.request.receptionist.CreateReservationRequest;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.CheckAvailabilityResponse;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.PagedResponse;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReservationResponseDTO;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.TableAvailabilityDTO;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.*;
import com.ByteKnights.com.resturarent_system.service.ReceptionistReservationService;
import com.ByteKnights.com.resturarent_system.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class ReceptionistReservationServiceImpl implements ReceptionistReservationService {

    private final ReservationRepository reservationRepository;
    private final RestaurantTableRepository tableRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final OrderRepository orderRepository;
    private final WebSocketNotificationService webSocketNotificationService;

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

        // Re-check the 1-hour gap for every selected table (guards against a stale picker / races).
        for (RestaurantTable t : tables) {
            List<Reservation> overlapping = reservationRepository.findOverlappingReservations(
                    t.getId(),
                    request.getReservationTime().minusHours(GAP_HOURS),
                    request.getEndTime().plusHours(GAP_HOURS));
            if (!overlapping.isEmpty()) {
                Reservation clash = overlapping.get(0);
                boolean directOverlap = clash.getReservationTime().isBefore(request.getEndTime())
                        && clash.getEndTime().isAfter(request.getReservationTime());
                throw new RuntimeException("Table " + t.getTableNumber() + (directOverlap
                        ? " is already reserved for this time slot"
                        : " needs at least a 1-hour gap between reservations"));
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
                .notes(request.getNotes())
                .status(ReservationStatus.PENDING)
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
            // Widen the window by the required gap so bookings too close to an existing reservation clash too.
            List<Reservation> nearby = reservationRepository.findOverlappingReservations(
                    t.getId(), start.minusHours(GAP_HOURS), end.plusHours(GAP_HOURS));
            TableAvailabilityDTO.TableAvailabilityDTOBuilder dto = TableAvailabilityDTO.builder()
                    .tableId(t.getId())
                    .tableNumber(t.getTableNumber())
                    .capacity(t.getCapacity());

            // Prefer a real time overlap; otherwise it's a gap-only clash (within an hour, no overlap).
            Reservation directClash = nearby.stream()
                    .filter(r -> r.getReservationTime().isBefore(end) && r.getEndTime().isAfter(start))
                    .findFirst().orElse(null);

            if (directClash != null) {
                dto.status("RESERVED")
                        .conflictStart(directClash.getReservationTime())
                        .conflictEnd(directClash.getEndTime())
                        .gapConflict(false);
            } else if (!nearby.isEmpty()) {
                Reservation clash = nearby.get(0);
                dto.status("RESERVED")
                        .conflictStart(clash.getReservationTime())
                        .conflictEnd(clash.getEndTime())
                        .gapConflict(true);
            } else if (t.getState() == TableStatus.OCCUPIED && bookingIsToday) {
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
                // If this occupancy came from a reservation, include its window too.
                if (t.getSeatedReservationId() != null) {
                    reservationRepository.findById(t.getSeatedReservationId()).ifPresent(sr -> {
                        dto.occupiedReservationStart(sr.getReservationTime());
                        dto.occupiedReservationEnd(sr.getEndTime());
                    });
                }
            } else {
                dto.status("FREE");
            }
            result.add(dto.build());
        }

        // Selectable = anything not RESERVED (occupied-today tables stay pickable at the receptionist's discretion).
        boolean possible = result.stream().anyMatch(d -> !"RESERVED".equals(d.getStatus()));

        return CheckAvailabilityResponse.builder()
                .possible(possible)
                .reason(possible ? null : "All tables are reserved for this time slot (1-hour gap applied).")
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
        if (r.getStatus() != ReservationStatus.PENDING) {
            throw new RuntimeException("This reservation is not active");
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
                .notes(r.getNotes())
                .status(r.getStatus().name())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
