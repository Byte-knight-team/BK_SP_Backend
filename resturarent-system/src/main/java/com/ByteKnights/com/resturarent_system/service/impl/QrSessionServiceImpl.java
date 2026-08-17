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
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of {@link QrSessionService} managing QR-based Dine-In customer
 * sessions.
 *
 * <p>
 * Key Responsibilities & Flow:
 * <ul>
 * <li><b>QR Code Verification:</b> Validates and decodes the static QR token
 * printed on restaurant tables.</li>
 * <li><b>Integrity Verification:</b> Ensures the target branch is active and
 * the table genuinely belongs to that branch.</li>
 * <li><b>Session Lifecycle Management:</b> Creates and tracks {@link QrSession}
 * entities (ACTIVE / ENDED).</li>
 * <li><b>JWT Session Token Minting:</b> Issues signed JWT session tokens
 * carrying {@code session_id},
 * {@code branch_id}, {@code table_id}, and {@code table_number} claims for
 * client-side API authentication.</li>
 * <li><b>High-Speed Validation with Redis:</b> Caches active session statuses
 * in Redis for sub-millisecond
 * validation, with an automatic fallback to MySQL in case of cache misses or
 * key eviction.</li>
 * </ul>
 */
@Service
public class QrSessionServiceImpl implements QrSessionService {

    private final QrSessionRepository qrSessionRepository;
    private final BranchRepository branchRepository;
    private final RestaurantTableRepository restaurantTableRepository;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.qr-session-expiration-ms}")
    private long sessionExpirationMs;

    public QrSessionServiceImpl(QrSessionRepository qrSessionRepository,
            BranchRepository branchRepository,
            RestaurantTableRepository restaurantTableRepository,
            StringRedisTemplate stringRedisTemplate) {
        this.qrSessionRepository = qrSessionRepository;
        this.branchRepository = branchRepository;
        this.restaurantTableRepository = restaurantTableRepository;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    @Transactional
    public QrSessionStartResponseData startSession(QrSessionStartRequest request) {
        validateRequest(request);

        // 1. Decode and verify the static QR token
        Claims qrClaims = parseToken(request.getQrToken());
        Long branchId = extractLongClaim(qrClaims, "branch_id");
        Long tableId = extractLongClaim(qrClaims, "table_id");
        Long qrId = extractLongClaim(qrClaims, "qr_id");

        // 2. Validate branch availability
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new QrSessionException(HttpStatus.NOT_FOUND, "Branch not found."));

        if (branch.getStatus() != BranchStatus.ACTIVE) {
            throw new QrSessionException(HttpStatus.CONFLICT, "Branch is not active.");
        }

        // 3. Validate table-branch integrity
        RestaurantTable table = restaurantTableRepository.findById(tableId)
                .orElseThrow(() -> new QrSessionException(HttpStatus.NOT_FOUND, "Table not found."));

        if (table.getBranch() == null || table.getBranch().getId() == null
                || !table.getBranch().getId().equals(branchId)) {
            throw new QrSessionException(HttpStatus.BAD_REQUEST, "QR token does not match the selected table.");
        }

        // 4. Persist the new active session entity
        QrSession qrSession = QrSession.builder()
                .branch(branch)
                .table(table)
                .status(QrSessionStatus.ACTIVE)
                .build();

        qrSession = qrSessionRepository.save(qrSession);

        // 5. Generate signed JWT session token with table & session claims
        Instant expiry = Instant.now().plusMillis(sessionExpirationMs);
        String sessionToken = generateSessionToken(qrSession.getId(), branchId, tableId, table.getTableNumber(), qrId,
                expiry);

        // 6. Cache active status in Redis with TTL for high-throughput request
        // validation
        stringRedisTemplate.opsForValue().set("qrsession:" + qrSession.getId(), QrSessionStatus.ACTIVE.name(),
                sessionExpirationMs, TimeUnit.MILLISECONDS);

        return QrSessionStartResponseData.builder()
                .sessionToken(sessionToken)
                .build();
    }

    /**
     * Terminate an active QR session (e.g., when the customer leaves or bill is
     * settled).
     *
     * <p>
     * Updates the session status in MySQL to {@link QrSessionStatus#ENDED}, records
     * the
     * end timestamp, and evicts the session key from Redis.
     *
     * @param sessionId The ID of the session to end.
     * @throws QrSessionException If the session does not exist in the database.
     */
    @Override
    @Transactional
    public void endSession(Long sessionId) {
        QrSession session = qrSessionRepository.findById(sessionId)
                .orElseThrow(() -> new QrSessionException(HttpStatus.NOT_FOUND, "QR session not found."));

        if (session.getStatus() == QrSessionStatus.ENDED) {
            return; // Already ended, idempotent no-op
        }

        session.setStatus(QrSessionStatus.ENDED);
        session.setEndedAt(java.time.LocalDateTime.now());
        qrSessionRepository.save(session);

        // Evict from Redis cache so subsequent API calls fail immediately
        stringRedisTemplate.delete("qrsession:" + sessionId);
    }

    @Override
    public void validateActiveSession(Long sessionId) {
        String cachedStatus = stringRedisTemplate.opsForValue().get("qrsession:" + sessionId);

        if (cachedStatus == null) {
            // Fallback to database check in case Redis was flushed or key expired naturally
            QrSession session = qrSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new QrSessionException(HttpStatus.NOT_FOUND, "QR session not found."));

            if (session.getStatus() != QrSessionStatus.ACTIVE) {
                throw new QrSessionException(HttpStatus.GONE,
                        "Your table session has ended. Please close this tab and rescan the QR code.");
            }

            // Re-cache active status in Redis
            stringRedisTemplate.opsForValue().set("qrsession:" + sessionId, QrSessionStatus.ACTIVE.name(),
                    sessionExpirationMs, TimeUnit.MILLISECONDS);
        } else if (!QrSessionStatus.ACTIVE.name().equals(cachedStatus)) {
            throw new QrSessionException(HttpStatus.GONE,
                    "Your table session has ended. Please close this tab and rescan the QR code.");
        }
    }

    /**
     * Validates that the start request and its QR token are non-null and non-blank.
     */
    private void validateRequest(QrSessionStartRequest request) {
        if (request == null || !StringUtils.hasText(request.getQrToken())) {
            throw new QrSessionException(HttpStatus.BAD_REQUEST, "qr_token is required.");
        }
    }

    /**
     * Cryptographically validates and extracts the payload claims from a signed JWT
     * token.
     *
     * @param token The signed JWT string.
     * @return The parsed {@link Claims} body.
     * @throws QrSessionException If the token signature is invalid or expired.
     */
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

    /**
     * Safely extracts a numeric claim from a JWT claims map and converts it to a
     * {@link Long}.
     *
     * @param claims    The parsed claims object.
     * @param claimName The name of the claim to extract.
     * @return The extracted Long value.
     * @throws QrSessionException If the claim is missing or null.
     */
    private Long extractLongClaim(Claims claims, String claimName) {
        Number claimValue = claims.get(claimName, Number.class);
        if (claimValue == null) {
            throw new QrSessionException(HttpStatus.BAD_REQUEST, "Missing QR token claim: " + claimName);
        }
        return claimValue.longValue();
    }

    private String generateSessionToken(Long sessionId, Long branchId, Long tableId, Integer tableNumber, Long qrId,
            Instant expiry) {
        Instant now = Instant.now();

        Map<String, Object> claims = Map.of(
                "session_id", sessionId,
                "branch_id", branchId,
                "table_id", tableId,
                "table_number", tableNumber,
                "qr_id", qrId,
                "status", QrSessionStatus.ACTIVE.name());

        return Jwts.builder()
                .subject("qr-session-" + sessionId)
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Decodes the Base64-encoded JWT secret key into a cryptographic
     * {@link SecretKey}.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}