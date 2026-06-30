package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.receptionist.CreateReservationRequest;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReservationResponseDTO;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.*;
import com.ByteKnights.com.resturarent_system.service.ReceptionistReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReceptionistReservationServiceImpl implements ReceptionistReservationService {

    private final ReservationRepository reservationRepository;
    private final RestaurantTableRepository tableRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;

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

        return ReservationResponseDTO.builder()
                .id(saved.getId())
                .tableId(table.getId())
                .tableNumber(table.getTableNumber())
                .customerName(saved.getCustomerName())
                .customerPhone(saved.getCustomerPhone())
                .reservationTime(saved.getReservationTime())
                .endTime(saved.getEndTime())
                .guestCount(saved.getGuestCount())
                .status(saved.getStatus().name())
                .createdAt(saved.getCreatedAt())
                .build();
    }
}
