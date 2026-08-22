package com.ByteKnights.com.resturarent_system.scheduler;

import com.ByteKnights.com.resturarent_system.entity.QrSession;
import com.ByteKnights.com.resturarent_system.entity.QrSessionStatus;
import com.ByteKnights.com.resturarent_system.repository.QrSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Background sweeper to automatically expire stale / abandoned QR table sessions
 * and purge their cached state from Redis.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QrSessionScheduler {

    private final QrSessionRepository qrSessionRepository;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.jwt.qr-session-expiration-ms:2400000}")
    private long sessionExpirationMs;

    @Scheduled(fixedRate = 120000) // Runs every 2 minutes
    @Transactional
    public void cleanupStaleQrSessions() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoffTime = now.minusSeconds(sessionExpirationMs / 1000);

        List<QrSession> staleSessions = qrSessionRepository.findByStatusAndStartedAtBefore(
                QrSessionStatus.ACTIVE, cutoffTime
        );

        if (staleSessions.isEmpty()) {
            return;
        }

        int cleanedCount = 0;
        for (QrSession session : staleSessions) {
            session.setStatus(QrSessionStatus.ENDED);
            session.setEndedAt(now);
            qrSessionRepository.save(session);

            // Purge Redis cache
            stringRedisTemplate.delete("qrsession:" + session.getId());
            cleanedCount++;
            log.info("Auto-expired stale QR session ID: {} for Table ID: {}", session.getId(),
                    session.getTable() != null ? session.getTable().getId() : "N/A");
        }

        log.info("Completed QR session cleanup sweeper: cleaned {} stale session(s).", cleanedCount);
    }
}
