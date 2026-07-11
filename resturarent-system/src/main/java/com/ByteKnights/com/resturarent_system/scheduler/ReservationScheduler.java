package com.ByteKnights.com.resturarent_system.scheduler;

import com.ByteKnights.com.resturarent_system.entity.Reservation;
import com.ByteKnights.com.resturarent_system.entity.ReservationStatus;
import com.ByteKnights.com.resturarent_system.entity.RestaurantTable;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationScheduler {

    private final ReservationRepository reservationRepository;
    private final RestaurantTableRepository tableRepository;
    private final WebSocketNotificationService webSocketNotificationService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    // Tracks reservations that have already been notified to prevent duplicate toasts
    private final Set<String> sentNotifications = new HashSet<>();

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void checkReservations() {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime oneHourStart = now.plusMinutes(59);
        LocalDateTime oneHourEnd = now.plusMinutes(61);

        LocalDateTime thirtyMinStart = now.plusMinutes(29);
        LocalDateTime thirtyMinEnd = now.plusMinutes(31);

        LocalDateTime fifteenMinStart = now.plusMinutes(14);
        LocalDateTime fifteenMinEnd = now.plusMinutes(16);

        List<Reservation> all = reservationRepository.findAll();

        for (Reservation r : all) {
            if (r.getStatus() != ReservationStatus.PENDING) continue;

            LocalDateTime reservationTime = r.getReservationTime();
            Long branchId = r.getBranch() != null ? r.getBranch().getId() : null;
            if (branchId == null) continue;
            Long reservationId = r.getId();

            // No-show auto-cancel: a PENDING reservation whose window has fully passed was never seated,
            // so cancel it and free its tables (each RESERVED table → AVAILABLE, unless another PENDING
            // reservation still holds it within the 15-minute window → then it stays RESERVED for that one).
            if (r.getEndTime() != null && r.getEndTime().isBefore(now)) {
                r.setStatus(ReservationStatus.CANCELLED);
                r.setCancelReason("Auto-cancelled — no-show (reservation time passed)");
                reservationRepository.save(r);
                boolean anyFreed = false;
                for (RestaurantTable table : r.getTables()) {
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
                if (anyFreed) webSocketNotificationService.broadcastTableUpdate(branchId);
                webSocketNotificationService.broadcastReservationUpdate(branchId);
                log.info("Auto-cancelled no-show reservation {} (window ended)", reservationId);
                continue;
            }

            // Representative table number for the reminder toast (lowest table number in the booking).
            Integer tableNumber = r.getTables().stream()
                    .map(RestaurantTable::getTableNumber)
                    .filter(java.util.Objects::nonNull)
                    .min(Integer::compareTo).orElse(null);
            String timeStr = reservationTime.format(TIME_FORMATTER);

            // 1-hour reminder — fire only once
            String key1hr = reservationId + "-1HR";
            if (!sentNotifications.contains(key1hr)
                    && !reservationTime.isBefore(oneHourStart)
                    && !reservationTime.isAfter(oneHourEnd)) {
                webSocketNotificationService.broadcastReservationReminder(branchId, "REMINDER_1HR", tableNumber, timeStr);
                sentNotifications.add(key1hr);
                log.info("1-hour reminder sent for table {} at {}", tableNumber, timeStr);
            }

            // 30-minute reminder — fire only once
            String key30min = reservationId + "-30MIN";
            if (!sentNotifications.contains(key30min)
                    && !reservationTime.isBefore(thirtyMinStart)
                    && !reservationTime.isAfter(thirtyMinEnd)) {
                webSocketNotificationService.broadcastReservationReminder(branchId, "REMINDER_30MIN", tableNumber, timeStr);
                sentNotifications.add(key30min);
                log.info("30-min reminder sent for table {} at {}", tableNumber, timeStr);
            }

            // 15-minute reminder + lock table — fire only once
            String key15min = reservationId + "-15MIN";
            if (!sentNotifications.contains(key15min)
                    && !reservationTime.isBefore(fifteenMinStart)
                    && !reservationTime.isAfter(fifteenMinEnd)) {
                webSocketNotificationService.broadcastReservationReminder(branchId, "REMINDER_15MIN", tableNumber, timeStr);
                sentNotifications.add(key15min);
                log.info("15-min reminder sent for table {} at {}", tableNumber, timeStr);

                // Lock every AVAILABLE table of this booking.
                boolean anyLocked = false;
                for (RestaurantTable table : r.getTables()) {
                    if (table.getState() == TableStatus.AVAILABLE) {
                        table.setState(TableStatus.RESERVED);
                        table.setStatusUpdatedAt(LocalDateTime.now());
                        tableRepository.save(table);
                        anyLocked = true;
                    }
                }
                if (anyLocked) {
                    webSocketNotificationService.broadcastTableUpdate(branchId);
                    log.info("Locked tables of reservation {} as RESERVED", reservationId);
                }
            }
        }
    }
}
