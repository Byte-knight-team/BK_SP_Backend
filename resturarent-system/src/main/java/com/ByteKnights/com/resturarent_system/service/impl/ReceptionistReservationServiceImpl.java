package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.receptionist.CancelReservationRequest;
import com.ByteKnights.com.resturarent_system.dto.request.receptionist.CreateReservationRequest;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReservationResponseDTO;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.*;
import com.ByteKnights.com.resturarent_system.service.ReceptionistReservationService;
import com.ByteKnights.com.resturarent_system.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReceptionistReservationServiceImpl implements ReceptionistReservationService {

    private final ReservationRepository reservationRepository;
    private final RestaurantTableRepository tableRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final WebSocketNotificationService webSocketNotificationService;

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

        List<Reservation> overlapping = reservationRepository.findOverlappingReservations(
                request.getTableId(),
                request.getReservationTime(),
                request.getEndTime()
        );

        if (!overlapping.isEmpty()) {
            throw new RuntimeException("Table is already reserved for this time slot");
        }

        Reservation reservation = Reservation.builder()
                .table(table)
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .reservationTime(request.getReservationTime())
                .endTime(request.getEndTime())
                .guestCount(request.getGuestCount())
                .status(ReservationStatus.CONFIRMED)
                .build();

        Reservation saved = reservationRepository.save(reservation);
        webSocketNotificationService.broadcastReservationUpdate(branchId);

        return toDTO(saved);
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

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelReason(request.getReason());
        reservationRepository.save(reservation);
        webSocketNotificationService.broadcastReservationUpdate(reservation.getTable().getBranch().getId());
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
                .status(r.getStatus().name())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
