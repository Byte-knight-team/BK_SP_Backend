package com.ByteKnights.com.resturarent_system.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class CustomerJwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    public String generateToken(Long userId, String email, String roleName) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtExpirationMs);

        Map<String, Object> claims = Map.of(
                "email", email,
                "roles", List.of(roleName),
                "userId", userId
        );

        return Jwts.builder()
                .subject(email)
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
