package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.receptionist.CancelReservationRequest;
import com.ByteKnights.com.resturarent_system.dto.request.receptionist.CheckAvailabilityRequest;
import com.ByteKnights.com.resturarent_system.dto.request.receptionist.CreateReservationRequest;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.CheckAvailabilityResponse;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReservationResponseDTO;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.TableAvailabilityDTO;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.*;
import com.ByteKnights.com.resturarent_system.service.ReceptionistReservationService;
import com.ByteKnights.com.resturarent_system.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private static final int MAX_WASTE_SEATS = 2;            // biggest allowed (capacity - party size)
    private static final int GAP_HOURS = 1;                  // required gap between two reservations on the same table

    @Override
    @Transactional
    public ReservationResponseDTO createReservation(CreateReservationRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));

        Long branchId = staff.getBranch().getId();

        RestaurantTable table = tableRepository.findById(request.getTableId())
                .orElseThrow(() -> new RuntimeException("Table not found"));

        if (!table.getBranch().getId().equals(branchId)) {
            throw new RuntimeException("Table does not belong to your branch");
        }

        // Reservation must be for a future time
        if (request.getReservationTime().isBefore(LocalDateTime.now().plusHours(MIN_RESERVATION_LEAD_HOURS))) {
            throw new RuntimeException("Reservation time must be in the future.");
        }

        // Enforce a 1-hour gap between reservations: widen the requested window by an hour on
        // each side, so a booking too close to an existing one (even without a direct overlap) clashes.
        List<Reservation> overlapping = reservationRepository.findOverlappingReservations(
                request.getTableId(),
                request.getReservationTime().minusHours(GAP_HOURS),
                request.getEndTime().plusHours(GAP_HOURS)
        );

        if (!overlapping.isEmpty()) {
            Reservation clash = overlapping.get(0);
            boolean directOverlap = clash.getReservationTime().isBefore(request.getEndTime())
                    && clash.getEndTime().isAfter(request.getReservationTime());
            throw new RuntimeException(directOverlap
                    ? "Table is already reserved for this time slot"
                    : "Table needs at least a 1-hour gap between reservations");
        }

        Reservation reservation = Reservation.builder()
                .table(table)
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .reservationTime(request.getReservationTime())
                .endTime(request.getEndTime())
                .guestCount(request.getGuestCount())
                .notes(request.getNotes())
                .status(ReservationStatus.PENDING)
                .build();

        Reservation saved = reservationRepository.save(reservation);

        // If the reservation starts within the 15-minute lock window and the table is free,
        // lock it to RESERVED right now — the scheduler's 15-min window would otherwise miss
        // a booking made less than 15 minutes ahead.
        if (table.getState() == TableStatus.AVAILABLE
                && !saved.getReservationTime().isAfter(LocalDateTime.now().plusMinutes(15))) {
            table.setState(TableStatus.RESERVED);
            table.setStatusUpdatedAt(LocalDateTime.now());
            tableRepository.save(table);
        }

        webSocketNotificationService.broadcastReservationUpdate(branchId);
        webSocketNotificationService.broadcastTableUpdate(branchId);

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
        int guestCount = request.getGuestCount() != null ? request.getGuestCount() : 0;

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
                    .earliestAllowed(earliestAllowed)
                    .tables(new ArrayList<>())
                    .build();
        }

        // 2. Size match — tables that fit the party
        List<RestaurantTable> tables = tableRepository.findByBranchId(branchId);
        List<RestaurantTable> fitting = tables.stream()
                .filter(t -> t.getCapacity() != null && t.getCapacity() >= guestCount)
                .toList();

        if (fitting.isEmpty()) {
            int maxCap = tables.stream()
                    .mapToInt(t -> t.getCapacity() != null ? t.getCapacity() : 0)
                    .max().orElse(0);
            return CheckAvailabilityResponse.builder()
                    .possible(false)
                    .reason("No table can seat " + guestCount + " guests. The largest table seats " + maxCap + ".")
                    .earliestAllowed(earliestAllowed)
                    .tables(new ArrayList<>())
                    .build();
        }

        // Prefer minimal waste; if only oversized tables fit, fall back to the smallest fitting one(s)
        List<RestaurantTable> eligible = fitting.stream()
                .filter(t -> (t.getCapacity() - guestCount) <= MAX_WASTE_SEATS)
                .toList();
        if (eligible.isEmpty()) {
            int smallestFitting = fitting.stream().mapToInt(RestaurantTable::getCapacity).min().orElse(0);
            eligible = fitting.stream()
                    .filter(t -> t.getCapacity() == smallestFitting)
                    .toList();
        }

        // 3. Tag each eligible table: FREE / RESERVED / OCCUPIED
        LocalDate today = LocalDate.now();
        List<TableAvailabilityDTO> result = new ArrayList<>();
        for (RestaurantTable t : eligible) {
            // Widen the window by the required gap so bookings too close to an existing reservation clash too.
            List<Reservation> nearby = reservationRepository.findOverlappingReservations(
                    t.getId(), start.minusHours(GAP_HOURS), end.plusHours(GAP_HOURS));
            TableAvailabilityDTO.TableAvailabilityDTOBuilder dto = TableAvailabilityDTO.builder()
                    .tableId(t.getId())
                    .tableNumber(t.getTableNumber())
                    .capacity(t.getCapacity());

            // Current occupancy only matters for a TODAY booking — for a future day,
            // whoever is seated now will be long gone, so ignore it.
            boolean bookingIsToday = start.toLocalDate().equals(today);

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
                long activeOrders = orderRepository.findByTableIdAndStatusNotIn(
                                t.getId(), List.of(OrderStatus.CANCELLED, OrderStatus.REJECTED, OrderStatus.ON_HOLD))
                        .stream()
                        .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().toLocalDate().equals(today))
                        .count();
                dto.status("OCCUPIED")
                        .occupiedSince(t.getStatusUpdatedAt())
                        .activeOrderCount((int) activeOrders);
            } else {
                dto.status("FREE");
            }
            result.add(dto.build());
        }

        boolean possible = result.stream().anyMatch(d -> !"RESERVED".equals(d.getStatus()));

        return CheckAvailabilityResponse.builder()
                .possible(possible)
                .reason(possible ? null : "All matching tables are already reserved for this time slot.")
                .earliestAllowed(earliestAllowed)
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

        RestaurantTable table = reservation.getTable();
        Long branchId = table.getBranch().getId();

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelReason(request.getReason());
        reservationRepository.save(reservation);

        // If the table was locked (RESERVED) for this now-cancelled slot, free it — unless another
        // PENDING reservation is still within the 15-minute window (then keep it held for that one).
        // Cancelling a merely upcoming reservation (table not RESERVED) never changes table status.
        if (table.getState() == TableStatus.RESERVED) {
            LocalDateTime now = LocalDateTime.now();
            boolean stillHasImmediateReservation = !reservationRepository
                    .findOverlappingReservations(table.getId(), now, now.plusMinutes(15))
                    .isEmpty();
            if (!stillHasImmediateReservation) {
                table.setState(TableStatus.AVAILABLE);
                table.setCurrentGuestCount(0);
                table.setStatusUpdatedAt(now);
                tableRepository.save(table);
                webSocketNotificationService.broadcastTableUpdate(branchId);
            }
        }

        webSocketNotificationService.broadcastReservationUpdate(branchId);
    }

    @Override
    @Transactional
    public void seatReservation(Long reservationId, Integer guestCount, String userEmail) {
        Long branchId = getBranchId(userEmail);

        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        RestaurantTable table = r.getTable();
        if (table.getBranch() == null || !table.getBranch().getId().equals(branchId)) {
            throw new RuntimeException("Reservation does not belong to your branch");
        }
        if (r.getStatus() != ReservationStatus.PENDING) {
            throw new RuntimeException("This reservation is not active");
        }

        // Seat the reserved party: occupy the table and mark the reservation completed
        table.setState(TableStatus.OCCUPIED);
        table.setCurrentGuestCount(guestCount != null ? guestCount : r.getGuestCount());
        table.setStatusUpdatedAt(LocalDateTime.now());
        tableRepository.save(table);

        r.setStatus(ReservationStatus.COMPLETED);
        reservationRepository.save(r);

        webSocketNotificationService.broadcastTableUpdate(branchId);
        webSocketNotificationService.broadcastReservationUpdate(branchId);
    }

    @Override
    public List<ReservationResponseDTO> getAllReservations(String userEmail) {
        Long branchId = getBranchId(userEmail);
        return reservationRepository.findAllByBranch(branchId).stream()
                .map(this::toDTO)
                .toList();
    }

    private Long getBranchId(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Staff staff = staffRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));
        return staff.getBranch().getId();
    }

    private ReservationResponseDTO toDTO(Reservation r) {
        return ReservationResponseDTO.builder()
                .id(r.getId())
                .tableId(r.getTable().getId())
                .tableNumber(r.getTable().getTableNumber())
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
