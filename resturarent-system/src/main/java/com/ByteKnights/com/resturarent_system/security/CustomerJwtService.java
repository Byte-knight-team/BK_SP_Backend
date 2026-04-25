package com.ByteKnights.com.resturarent_system.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class CustomerJwtService {

    //extract secreat keys and time

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    //method to genarate token

    public String generateToken(Long userId, String email, String roleName) {

        //take current time and make expiration time
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtExpirationMs);

        //create custom data fileds for token

        Map<String, Object> claims = Map.of(
                "email", email,
                "roles", List.of(roleName),
                "userId", userId
        );

        //build and return the token

        return Jwts.builder()
                .subject(email)
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
            .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    //helper method to get correct formated cryptrograpy key

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
