package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.customer.QrSessionStartRequest;
import com.ByteKnights.com.resturarent_system.dto.response.customer.QrSessionStartResponseData;
import com.ByteKnights.com.resturarent_system.entity.Branch;
import com.ByteKnights.com.resturarent_system.entity.QrSession;
import com.ByteKnights.com.resturarent_system.entity.QrSessionStatus;
import com.ByteKnights.com.resturarent_system.entity.RestaurantTable;
import com.ByteKnights.com.resturarent_system.entity.BranchStatus;
import com.ByteKnights.com.resturarent_system.exception.QrSessionException;
import com.ByteKnights.com.resturarent_system.repository.BranchRepository;
import com.ByteKnights.com.resturarent_system.repository.QrSessionRepository;
import com.ByteKnights.com.resturarent_system.repository.RestaurantTableRepository;
import com.ByteKnights.com.resturarent_system.service.QrSessionService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class QrSessionServiceImpl implements QrSessionService {

    private final QrSessionRepository qrSessionRepository;
    private final BranchRepository branchRepository;
    private final RestaurantTableRepository restaurantTableRepository;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.qr-session-expiration-ms}")
    private long sessionExpirationMs;

    public QrSessionServiceImpl(QrSessionRepository qrSessionRepository,
                                BranchRepository branchRepository,
                                RestaurantTableRepository restaurantTableRepository) {
        this.qrSessionRepository = qrSessionRepository;
        this.branchRepository = branchRepository;
        this.restaurantTableRepository = restaurantTableRepository;
    }

    @Override
    @Transactional
    public QrSessionStartResponseData startSession(QrSessionStartRequest request) {
        validateRequest(request);

        Claims qrClaims = parseToken(request.getQrToken());
        Long branchId = extractLongClaim(qrClaims, "branch_id");
        Long tableId = extractLongClaim(qrClaims, "table_id");
        Long qrId = extractLongClaim(qrClaims, "qr_id");

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new QrSessionException(HttpStatus.NOT_FOUND, "Branch not found."));

        if (branch.getStatus() != BranchStatus.ACTIVE) {
            throw new QrSessionException(HttpStatus.CONFLICT, "Branch is not active.");
        }

        RestaurantTable table = restaurantTableRepository.findById(tableId)
                .orElseThrow(() -> new QrSessionException(HttpStatus.NOT_FOUND, "Table not found."));

        if (table.getBranch() == null || table.getBranch().getId() == null || !table.getBranch().getId().equals(branchId)) {
            throw new QrSessionException(HttpStatus.BAD_REQUEST, "QR token does not match the selected table.");
        }

        QrSession qrSession = QrSession.builder()
                .branch(branch)
                .table(table)
                .status(QrSessionStatus.ACTIVE)
                .build();

        qrSession = qrSessionRepository.save(qrSession);

    Instant expiry = Instant.now().plusMillis(sessionExpirationMs);
    String sessionToken = generateSessionToken(qrSession.getId(), branchId, tableId, qrId, expiry);

        return QrSessionStartResponseData.builder()
                .sessionToken(sessionToken)
                .build();
    }

    @Override
    @Transactional
    public void endSession(Long sessionId) {
        QrSession session = qrSessionRepository.findById(sessionId)
                .orElseThrow(() -> new QrSessionException(HttpStatus.NOT_FOUND, "QR session not found."));

        if (session.getStatus() == QrSessionStatus.ENDED) {
            return; // Already ended, no-op
        }

        session.setStatus(QrSessionStatus.ENDED);
        session.setEndedAt(java.time.LocalDateTime.now());
        qrSessionRepository.save(session);
    }

    @Override
    public void validateActiveSession(Long sessionId) {
        QrSession session = qrSessionRepository.findById(sessionId)
                .orElseThrow(() -> new QrSessionException(HttpStatus.NOT_FOUND, "QR session not found."));

        if (session.getStatus() != QrSessionStatus.ACTIVE) {
            throw new QrSessionException(HttpStatus.GONE,
                    "Your table session has ended. Please close this tab and rescan the QR code.");
        }
    }

    private void validateRequest(QrSessionStartRequest request) {
        if (request == null || !StringUtils.hasText(request.getQrToken())) {
            throw new QrSessionException(HttpStatus.BAD_REQUEST, "qr_token is required.");
        }
    }

    private Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception ex) {
            throw new QrSessionException(HttpStatus.UNAUTHORIZED, "Invalid QR token.");
        }
    }

    private Long extractLongClaim(Claims claims, String claimName) {
        Number claimValue = claims.get(claimName, Number.class);
        if (claimValue == null) {
            throw new QrSessionException(HttpStatus.BAD_REQUEST, "Missing QR token claim: " + claimName);
        }
        return claimValue.longValue();
    }

    private String generateSessionToken(Long sessionId, Long branchId, Long tableId, Long qrId, Instant expiry) {
        Instant now = Instant.now();

        Map<String, Object> claims = Map.of(
                "session_id", sessionId,
                "branch_id", branchId,
                "table_id", tableId,
                "qr_id", qrId,
                "status", QrSessionStatus.ACTIVE.name()
        );

        return Jwts.builder()
                .subject("qr-session-" + sessionId)
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}