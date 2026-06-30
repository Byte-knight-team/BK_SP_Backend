package com.ByteKnights.com.resturarent_system.scheduler;

import com.ByteKnights.com.resturarent_system.entity.Reservation;
import com.ByteKnights.com.resturarent_system.entity.ReservationStatus;
import com.ByteKnights.com.resturarent_system.entity.TableStatus;
import com.ByteKnights.com.resturarent_system.repository.ReservationRepository;
import com.ByteKnights.com.resturarent_system.repository.RestaurantTableRepository;
import com.ByteKnights.com.resturarent_system.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationScheduler {

    private final ReservationRepository reservationRepository;
    private final RestaurantTableRepository tableRepository;
    private final WebSocketNotificationService webSocketNotificationService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void checkReservations() {
        LocalDateTime now = LocalDateTime.now();

        // Window: reservations starting between 59–61 min from now → 1-hour reminder
        LocalDateTime oneHourStart = now.plusMinutes(59);
        LocalDateTime oneHourEnd = now.plusMinutes(61);

        // Window: reservations starting between 14–16 min from now → 15-min reminder + lock
        LocalDateTime fifteenMinStart = now.plusMinutes(14);
        LocalDateTime fifteenMinEnd = now.plusMinutes(16);

        List<Reservation> all = reservationRepository.findAll();

        for (Reservation r : all) {
            if (r.getStatus() != ReservationStatus.CONFIRMED) continue;

            LocalDateTime reservationTime = r.getReservationTime();
            Long branchId = r.getTable().getBranch().getId();
            Integer tableNumber = r.getTable().getTableNumber();
            String timeStr = reservationTime.format(TIME_FORMATTER);

            // 1-hour reminder
            if (!reservationTime.isBefore(oneHourStart) && !reservationTime.isAfter(oneHourEnd)) {
                webSocketNotificationService.broadcastReservationReminder(
                        branchId, "REMINDER_1HR", tableNumber, timeStr);
                log.info("1-hour reminder sent for table {} at {}", tableNumber, timeStr);
            }

            // 15-minute reminder + lock table
            if (!reservationTime.isBefore(fifteenMinStart) && !reservationTime.isAfter(fifteenMinEnd)) {
                webSocketNotificationService.broadcastReservationReminder(
                        branchId, "REMINDER_15MIN", tableNumber, timeStr);
                log.info("15-min reminder sent for table {} at {}", tableNumber, timeStr);

                // Lock the table only if it's currently AVAILABLE (don't disturb OCCUPIED tables)
                var table = r.getTable();
                if (table.getState() == TableStatus.AVAILABLE) {
                    table.setState(TableStatus.RESERVED);
                    table.setStatusUpdatedAt(LocalDateTime.now());
                    tableRepository.save(table);
                    webSocketNotificationService.broadcastTableUpdate(branchId);
                    log.info("Table {} locked as RESERVED for upcoming reservation", tableNumber);
                }
            }
        }
    }
}
