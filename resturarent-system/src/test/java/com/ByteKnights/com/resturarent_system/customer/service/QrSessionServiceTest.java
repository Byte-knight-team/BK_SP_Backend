package com.ByteKnights.com.resturarent_system.customer.service;

import com.ByteKnights.com.resturarent_system.dto.request.customer.QrSessionStartRequest;
import com.ByteKnights.com.resturarent_system.dto.response.customer.QrSessionStartResponseData;
import com.ByteKnights.com.resturarent_system.entity.Branch;
import com.ByteKnights.com.resturarent_system.entity.BranchStatus;
import com.ByteKnights.com.resturarent_system.entity.QrSession;
import com.ByteKnights.com.resturarent_system.entity.QrSessionStatus;
import com.ByteKnights.com.resturarent_system.entity.RestaurantTable;
import com.ByteKnights.com.resturarent_system.exception.QrSessionException;
import com.ByteKnights.com.resturarent_system.repository.BranchRepository;
import com.ByteKnights.com.resturarent_system.repository.QrSessionRepository;
import com.ByteKnights.com.resturarent_system.repository.RestaurantTableRepository;
import com.ByteKnights.com.resturarent_system.service.impl.QrSessionServiceImpl;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class QrSessionServiceTest {

    @Mock
    private QrSessionRepository qrSessionRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private RestaurantTableRepository restaurantTableRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private QrSessionServiceImpl qrSessionService;

    private String validQrToken;
    private Branch branch;
    private RestaurantTable table;
    private QrSession activeSession;

    private final String secretKey = "bXlTZWNyZXRLZXlGb3JKd3RUb2tlbkdlbmVyYXRpb25NdXN0QmVBdExlYXN0MzJCeXRlc0xvbmc="; // Base64 for 32+ bytes

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(qrSessionService, "jwtSecret", secretKey);
        ReflectionTestUtils.setField(qrSessionService, "sessionExpirationMs", 3600000L); // 1 hour

        branch = new Branch();
        branch.setId(1L);
        branch.setStatus(BranchStatus.ACTIVE);

        table = new RestaurantTable();
        table.setId(10L);
        table.setTableNumber(5);
        table.setBranch(branch);

        activeSession = new QrSession();
        activeSession.setId(100L);
        activeSession.setStatus(QrSessionStatus.ACTIVE);
        activeSession.setBranch(branch);
        activeSession.setTable(table);

        // Generate a valid token simulating what QrCodeService creates
        validQrToken = Jwts.builder()
                .subject("table-qr-1")
                .claims(Map.of(
                        "branch_id", 1L,
                        "table_id", 10L,
                        "qr_id", 50L
                ))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(Keys.hmacShaKeyFor(io.jsonwebtoken.io.Decoders.BASE64.decode(secretKey)), Jwts.SIG.HS256)
                .compact();
    }

    @Test
    void testStartSession_Success() {
        // Arrange
        QrSessionStartRequest request = new QrSessionStartRequest();
        request.setQrToken(validQrToken);

        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(restaurantTableRepository.findById(10L)).thenReturn(Optional.of(table));
        
        when(qrSessionRepository.save(any(QrSession.class))).thenAnswer(invocation -> {
            QrSession session = invocation.getArgument(0);
            session.setId(100L); // Simulate generated ID
            return session;
        });
        
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        QrSessionStartResponseData response = qrSessionService.startSession(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getSessionToken());
        
        verify(qrSessionRepository).save(any(QrSession.class));
        // Verify Redis is updated with ACTIVE status
        verify(valueOperations).set(eq("qrsession:100"), eq("ACTIVE"), eq(3600000L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void testValidateActiveSession_InRedis() {
        // Arrange
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("qrsession:100")).thenReturn("ACTIVE");

        // Act & Assert
        assertDoesNotThrow(() -> qrSessionService.validateActiveSession(100L));
        
        // Ensure database fallback was NEVER called because Redis had the cache
        verify(qrSessionRepository, never()).findById(anyLong());
    }

    @Test
    void testValidateActiveSession_FallbackToDb() {
        // Arrange
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("qrsession:100")).thenReturn(null); // Redis eviction simulation
        
        when(qrSessionRepository.findById(100L)).thenReturn(Optional.of(activeSession));

        // Act & Assert
        assertDoesNotThrow(() -> qrSessionService.validateActiveSession(100L));
        
        // Ensure DB was queried and Redis was repopulated
        verify(qrSessionRepository).findById(100L);
        verify(valueOperations).set(eq("qrsession:100"), eq("ACTIVE"), eq(3600000L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void testValidateActiveSession_Ended() {
        // Arrange
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("qrsession:100")).thenReturn("ENDED");

        // Act & Assert
        QrSessionException exception = assertThrows(QrSessionException.class, () -> {
            qrSessionService.validateActiveSession(100L);
        });
        
        assertEquals("Your table session has ended. Please close this tab and rescan the QR code.", exception.getMessage());
    }
}
