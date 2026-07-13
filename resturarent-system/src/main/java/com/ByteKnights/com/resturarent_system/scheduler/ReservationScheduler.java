package com.ByteKnights.com.resturarent_system.scheduler;

import com.ByteKnights.com.resturarent_system.entity.Reservation;
import com.ByteKnights.com.resturarent_system.entity.ReservationStatus;
import com.ByteKnights.com.resturarent_system.entity.RestaurantTable;
import com.ByteKnights.com.resturarent_system.entity.TableStatus;
import com.ByteKnights.com.resturarent_system.repository.ReservationRepository;
import com.ByteKnights.com.resturarent_system.repository.RestaurantTableRepository;
import com.ByteKnights.com.resturarent_system.service.email.EmailService;
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
    private final EmailService emailService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    // Tracks reservations that have already been notified to prevent duplicate
    // toasts
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
            Long branchId = r.getBranch() != null ? r.getBranch().getId() : null;
            Long reservationId = r.getId();

            if (r.getStatus() == ReservationStatus.CONFIRMED && r.getPaymentDeadline() != null
                    && r.getPaymentDeadline().isBefore(now)) {
                r.setStatus(ReservationStatus.EXPIRED);
                reservationRepository.save(r);

                // Payment window closed → free any tables this booking was holding, unless
                // another CONFIRMED/PAID booking still holds them within the 15-minute window.
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
                if (branchId != null) {
                    if (anyFreed) webSocketNotificationService.broadcastTableUpdate(branchId);
                    webSocketNotificationService.broadcastReservationUpdate(branchId);
                }
                if (r.getCustomer() != null && r.getCustomer().getUser() != null) {
                    webSocketNotificationService.broadcastReservationStatusToCustomer(r.getCustomer().getUser().getId(),
                            reservationId, "EXPIRED");
                    try {
                        emailService.sendSimpleEmail(r.getCustomer().getUser().getEmail(), "Reservation Expired",
                                "Your reservation at " + r.getBranch().getName()
                                        + " has expired because the payment was not completed within the allowed time window.");
                    } catch (Exception e) {
                        log.error("Failed to send expiration email for reservation {}", reservationId, e);
                    }
                }
                log.info("Auto-expired unpaid confirmed reservation {}", reservationId);
                continue;
            }

            if (r.getStatus() != ReservationStatus.PAID)
                continue;

            if (branchId == null)
                continue;

            LocalDateTime reservationTime = r.getReservationTime();

            // NOTE: no-show is handled MANUALLY by the receptionist (they may choose to wait a
            // while for late guests), so there is deliberately NO auto-cancel here — a PAID
            // booking stays PAID until it is either seated or cancelled by staff.

            // Representative table number for the reminder toast (lowest table number in
            // the booking).
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
                webSocketNotificationService.broadcastReservationReminder(branchId, "REMINDER_1HR", tableNumber,
                        timeStr);
                sentNotifications.add(key1hr);
                log.info("1-hour reminder sent for table {} at {}", tableNumber, timeStr);
            }

            // 30-minute reminder — fire only once
            String key30min = reservationId + "-30MIN";
            if (!sentNotifications.contains(key30min)
                    && !reservationTime.isBefore(thirtyMinStart)
                    && !reservationTime.isAfter(thirtyMinEnd)) {
                webSocketNotificationService.broadcastReservationReminder(branchId, "REMINDER_30MIN", tableNumber,
                        timeStr);
                sentNotifications.add(key30min);
                log.info("30-min reminder sent for table {} at {}", tableNumber, timeStr);
            }

            // 15-minute reminder + lock table — fire only once
            String key15min = reservationId + "-15MIN";
            if (!sentNotifications.contains(key15min)
                    && !reservationTime.isBefore(fifteenMinStart)
                    && !reservationTime.isAfter(fifteenMinEnd)) {
                webSocketNotificationService.broadcastReservationReminder(branchId, "REMINDER_15MIN", tableNumber,
                        timeStr);
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

            // #5 GUEST LATE — the slot has started but the guest still isn't seated (still
            // PAID;
            // past-end no-shows were auto-cancelled above, so this window is still active).
            // Fire once.
            String keyLate = reservationId + "-LATE";
            if (!sentNotifications.contains(keyLate) && reservationTime.isBefore(now)) {
                webSocketNotificationService.broadcastReservationReminder(branchId, "GUEST_LATE", tableNumber, timeStr);
                sentNotifications.add(keyLate);
                log.info("Guest-late notice for table {} reservation {}", tableNumber, reservationId);
            }
        }

        // #6 TIME'S UP — a table occupied for a reservation whose reserved window has
        // ended. Notify once
        // per booking (the reserved time is over — ask them to leave / clear the
        // table).
        for (RestaurantTable table : tableRepository.findAll()) {
            if (table.getState() != TableStatus.OCCUPIED || table.getSeatedReservationId() == null)
                continue;
            Reservation sr = reservationRepository.findById(table.getSeatedReservationId()).orElse(null);
            if (sr == null || sr.getEndTime() == null || !sr.getEndTime().isBefore(now))
                continue;
            String keyUp = "TIMEUP-" + sr.getId();
            if (sentNotifications.contains(keyUp))
                continue;
            Long upBranchId = table.getBranch() != null ? table.getBranch().getId() : null;
            if (upBranchId == null)
                continue;
            webSocketNotificationService.broadcastReservationReminder(
                    upBranchId, "TIME_UP", table.getTableNumber(), sr.getEndTime().format(TIME_FORMATTER));
            sentNotifications.add(keyUp);
            log.info("Time's-up notice for table {} reservation {}", table.getTableNumber(), sr.getId());
        }
    }
}
