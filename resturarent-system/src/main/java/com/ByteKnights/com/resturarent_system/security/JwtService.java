package com.ByteKnights.com.resturarent_system.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    /*
     * Old token generator kept for safety.
     * If any old code still calls generateToken(userId, email, role),
     * it will still work and branch values will be null.
     */
    public String generateToken(Long userId, String email, String role) {
        return generateToken(userId, email, role, null, null);
    }

    /*
     * Main token generator.
     *
     * JWT payload will contain:
     * - role
     * - userId
     * - branchId
     * - branchName
     * - sub/email
     * - iat
     * - exp
     *
     * Do not store sensitive data like password, salary, phone,
     * temporary password, or full user details in the JWT.
     */
    public String generateToken(
            Long userId,
            String email,
            String role,
            Long branchId,
            String branchName
    ) {
        Map<String, Object> claims = new HashMap<>();

        claims.put("role", role);
        claims.put("userId", userId);
        claims.put("branchId", branchId);
        claims.put("branchName", branchName);

        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = extractAllClaims(token);

        /*
         * userId was stored as a number.
         * Reading as Number is safer than directly reading as Long,
         * because some JWT parsers may deserialize small numbers as Integer.
         */
        Number userId = claims.get("userId", Number.class);
        return userId == null ? null : userId.longValue();
    }

    public Long getBranchIdFromToken(String token) {
        Claims claims = extractAllClaims(token);

        Number branchId = claims.get("branchId", Number.class);
        return branchId == null ? null : branchId.longValue();
    }

    public String getBranchNameFromToken(String token) {
        return extractAllClaims(token).get("branchName", String.class);
    }

    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isTokenValid(String token, String email) {
        final String extractedEmail = extractEmail(token);
        return extractedEmail.equals(email) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes;

        try {
            keyBytes = Decoders.BASE64.decode(jwtSecret);
        } catch (Exception e) {
            keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }
}