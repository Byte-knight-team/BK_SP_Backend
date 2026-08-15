package com.ByteKnights.com.resturarent_system.customer.service;

import com.ByteKnights.com.resturarent_system.dto.response.customer.CustomerLoginResponseData;
import com.ByteKnights.com.resturarent_system.entity.Customer;
import com.ByteKnights.com.resturarent_system.entity.Role;
import com.ByteKnights.com.resturarent_system.entity.User;
import com.ByteKnights.com.resturarent_system.exception.CustomerAuthException;
import com.ByteKnights.com.resturarent_system.repository.CustomerRepository;
import com.ByteKnights.com.resturarent_system.repository.RoleRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import com.ByteKnights.com.resturarent_system.security.CustomerJwtService;
import com.ByteKnights.com.resturarent_system.service.impl.CustomerAuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomerAuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CustomerJwtService customerJwtService;

    @InjectMocks
    private CustomerAuthServiceImpl customerAuthService;

    private User mockUser;
    private Customer mockCustomer;
    private Role customerRole;

    @BeforeEach
    void setUp() {
        customerRole = new Role();
        customerRole.setId(1L);
        customerRole.setName("CUSTOMER");

        mockUser = new User();
        mockUser.setId(10L);
        mockUser.setPhone("1234567890");
        mockUser.setRole(customerRole);
        mockUser.setIsActive(true);

        mockCustomer = new Customer();
        mockCustomer.setId(100L);
        mockCustomer.setUser(mockUser);
    }

    @Test
    void testRequestOtp_NewUser_Success() {
        // Arrange
        String phone = "1234567890";
        when(userRepository.findByPhone(phone)).thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        customerAuthService.requestOtp(phone);

        // Assert
        verify(userRepository).save(any(User.class));
        verify(valueOperations).set(eq("otp:" + phone), anyString(), eq(5L), eq(TimeUnit.MINUTES));
    }

    @Test
    void testRequestOtp_ExistingActiveUser_Success() {
        // Arrange
        String phone = "1234567890";
        when(userRepository.findByPhone(phone)).thenReturn(Optional.of(mockUser));
        when(customerRepository.findByUser(mockUser)).thenReturn(Optional.of(mockCustomer));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        customerAuthService.requestOtp(phone);

        // Assert
        verify(userRepository, never()).save(any(User.class)); // Shouldn't save new user
        verify(valueOperations).set(eq("otp:" + phone), anyString(), eq(5L), eq(TimeUnit.MINUTES));
    }

    @Test
    void testVerifyOtp_Success() {
        // Arrange
        String phone = "1234567890";
        String validOtp = "1234";
        
        when(userRepository.findByPhone(phone)).thenReturn(Optional.of(mockUser));
        when(customerRepository.findByUser(mockUser)).thenReturn(Optional.of(mockCustomer));
        
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:" + phone)).thenReturn(validOtp);
        when(customerJwtService.generateToken(anyLong(), anyString(), anyString())).thenReturn("jwtToken");

        // Act
        CustomerLoginResponseData response = customerAuthService.verifyOtp(phone, validOtp, null);

        // Assert
        assertNotNull(response);
        assertEquals("jwtToken", response.getToken());
        
        // Verify Redis cleanup
        verify(stringRedisTemplate).delete("otp:" + phone);
        
        // Verify phone verified flag was updated
        assertTrue(mockCustomer.getPhoneVerified());
        verify(customerRepository).save(mockCustomer);
    }

    @Test
    void testVerifyOtp_InvalidOrExpired() {
        // Arrange
        String phone = "1234567890";
        String wrongOtp = "9999";
        String cachedOtp = "1234";
        
        when(userRepository.findByPhone(phone)).thenReturn(Optional.of(mockUser));
        when(customerRepository.findByUser(mockUser)).thenReturn(Optional.of(mockCustomer));
        
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:" + phone)).thenReturn(cachedOtp);

        // Act & Assert
        CustomerAuthException exception = assertThrows(CustomerAuthException.class, () -> {
            customerAuthService.verifyOtp(phone, wrongOtp, null);
        });
        
        assertTrue(exception.getMessage().contains("OTP code has expired or is invalid"));
        verify(stringRedisTemplate, never()).delete(anyString());
    }
}
