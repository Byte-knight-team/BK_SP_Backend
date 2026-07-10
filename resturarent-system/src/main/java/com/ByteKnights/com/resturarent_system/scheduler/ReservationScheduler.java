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
            Long branchId = r.getTable().getBranch().getId();
            Integer tableNumber = r.getTable().getTableNumber();
            String timeStr = reservationTime.format(TIME_FORMATTER);
            Long reservationId = r.getId();

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

                // Lock the table only if currently AVAILABLE
                var table = r.getTable();
                if (table.getState() == TableStatus.AVAILABLE) {
                    table.setState(TableStatus.RESERVED);
                    table.setStatusUpdatedAt(LocalDateTime.now());
                    tableRepository.save(table);
                    webSocketNotificationService.broadcastTableUpdate(branchId);
                    log.info("Table {} locked as RESERVED", tableNumber);
                }
            }
        }
    }
}
