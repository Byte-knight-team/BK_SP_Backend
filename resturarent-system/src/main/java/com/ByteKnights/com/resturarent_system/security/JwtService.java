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

/*
    JwtService handles JWT token creation, validation and extracting data from tokens.
 */
@Service
public class JwtService {

    /*
     * Secret key used to sign and verify JWT tokens.
     */
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    /*
     * Token expiration time in milliseconds.
     */
    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    /*
     * Generates a staff JWT token after successful login.
     */
    public String generateToken(
            Long userId,
            String email,
            String role,
            Long branchId,
            String branchName) {
        Map<String, Object> claims = new HashMap<>();

        /*
         * Custom data stored inside the JWT payload.
         */
        claims.put("role", role);
        claims.put("userId", userId);
        claims.put("branchId", branchId);
        claims.put("branchName", branchName);

        /*
         * Build and sign the JWT token.
         */
        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /*
     * Generates a JWT token for QR/session related use cases.
     */
    public String generateQrToken(Map<String, Object> claims, String subject, long expirationMs) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /*
     * Verifies the token signature and parses the token to get all claims.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /*
     * Creates the signing key used for JWT signing and verification.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes;

        try {
            keyBytes = Decoders.BASE64.decode(jwtSecret);
        } catch (Exception e) {
            keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    /*
     * Checks whether the token is valid and not expired.
     */
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /*
     * Generic method to extract any claim from the token.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /*
     * Checks token validity against a specific email.
     */
    public boolean isTokenValid(String token, String email) {
        final String extractedEmail = extractEmail(token);
        return extractedEmail.equals(email) && !isTokenExpired(token);
    }

    /*
     * Extracts email from the token subject.
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /*
     * Extracts role from token claims.
     */
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    /*
     * Extracts userId from token.
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = extractAllClaims(token);

        /*
         * Read as Number because JWT may store numbers as Integer or Long depending on
         * size.
         */
        Number userId = claims.get("userId", Number.class);
        return userId == null ? null : userId.longValue();
    }

    /*
     * Extracts branchId from token.
     */
    public Long getBranchIdFromToken(String token) {
        Claims claims = extractAllClaims(token);

        Number branchId = claims.get("branchId", Number.class);
        return branchId == null ? null : branchId.longValue();
    }

    /*
     * Extracts branchName from token.
     */
    public String getBranchNameFromToken(String token) {
        return extractAllClaims(token).get("branchName", String.class);
    }

    /*
     * Extracts expiration date from the token.
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /*
     * Checks whether the token expiry time has passed.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

}