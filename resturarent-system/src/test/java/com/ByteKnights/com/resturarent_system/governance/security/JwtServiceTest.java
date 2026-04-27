package com.ByteKnights.com.resturarent_system.governance.security;

import com.ByteKnights.com.resturarent_system.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtService.
 *
 * These tests verify JWT generation, claim extraction, and validation.
 * No Spring Boot application context is started.
 */
class JwtServiceTest {

    private JwtService jwtService;

    /**
     * Test secret must be at least 32 characters for HMAC signing.
     */
    private static final String TEST_SECRET =
            "this-is-a-test-secret-key-for-jwt-service-123456";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        /*
         * JwtService normally reads these values from application.properties.
         * In a unit test, we inject them manually using ReflectionTestUtils.
         */
        ReflectionTestUtils.setField(jwtService, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 3600000L);
    }

    @Test
    void generateToken_shouldCreateTokenWithUserRoleAndBranchClaims() {
        // Act
        String token = jwtService.generateToken(
                10L,
                "admin@test.com",
                "ADMIN",
                2L,
                "Branch 02"
        );

        // Assert
        assertNotNull(token);
        assertFalse(token.isBlank());

        assertEquals("admin@test.com", jwtService.extractEmail(token));
        assertEquals("ADMIN", jwtService.extractRole(token));
        assertEquals(10L, jwtService.getUserIdFromToken(token));
        assertEquals(2L, jwtService.getBranchIdFromToken(token));
        assertEquals("Branch 02", jwtService.getBranchNameFromToken(token));
        assertTrue(jwtService.validateToken(token));
    }

    @Test
    void generateToken_oldMethod_shouldCreateTokenWithNullBranchClaims() {
        // Act
        String token = jwtService.generateToken(
                1L,
                "superadmin@test.com",
                "SUPER_ADMIN"
        );

        // Assert
        assertNotNull(token);
        assertEquals("superadmin@test.com", jwtService.extractEmail(token));
        assertEquals("SUPER_ADMIN", jwtService.extractRole(token));
        assertEquals(1L, jwtService.getUserIdFromToken(token));

        /*
         * SUPER_ADMIN is global, so branch details can be null.
         */
        assertNull(jwtService.getBranchIdFromToken(token));
        assertNull(jwtService.getBranchNameFromToken(token));
        assertTrue(jwtService.validateToken(token));
    }

    @Test
    void isTokenValid_shouldReturnTrue_whenEmailMatchesAndTokenNotExpired() {
        // Arrange
        String token = jwtService.generateToken(
                10L,
                "admin@test.com",
                "ADMIN",
                2L,
                "Branch 02"
        );

        // Act
        boolean result = jwtService.isTokenValid(token, "admin@test.com");

        // Assert
        assertTrue(result);
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenEmailDoesNotMatch() {
        // Arrange
        String token = jwtService.generateToken(
                10L,
                "admin@test.com",
                "ADMIN",
                2L,
                "Branch 02"
        );

        // Act
        boolean result = jwtService.isTokenValid(token, "wrong@test.com");

        // Assert
        assertFalse(result);
    }

    @Test
    void validateToken_shouldReturnFalse_whenTokenIsInvalid() {
        // Arrange
        String invalidToken = "this.is.not.a.valid.jwt";

        // Act
        boolean result = jwtService.validateToken(invalidToken);

        // Assert
        assertFalse(result);
    }

    @Test
    void validateToken_shouldReturnFalse_whenTokenIsExpired() {
        // Arrange
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", -1000L);

        String expiredToken = jwtService.generateToken(
                10L,
                "admin@test.com",
                "ADMIN",
                2L,
                "Branch 02"
        );

        // Act
        boolean result = jwtService.validateToken(expiredToken);

        // Assert
        assertFalse(result);
    }

    @Test
    void generateQrToken_shouldCreateTokenWithCustomClaims() {
        // Arrange
        Map<String, Object> claims = new HashMap<>();
        claims.put("tableId", 5L);
        claims.put("branchId", 2L);

        // Act
        String token = jwtService.generateQrToken(
                claims,
                "QR_TABLE_SESSION",
                3600000L
        );

        // Assert
        assertNotNull(token);
        assertEquals("QR_TABLE_SESSION", jwtService.extractEmail(token));
        assertTrue(jwtService.validateToken(token));
    }
}