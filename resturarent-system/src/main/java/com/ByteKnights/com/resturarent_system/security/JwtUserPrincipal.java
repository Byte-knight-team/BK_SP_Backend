package com.ByteKnights.com.resturarent_system.security;

/**
 * Lightweight principal for JWT authentication
 */
public record JwtUserPrincipal(
        Long id,
        String email,
        String role,
        Boolean passwordChanged
) {}