package com.ByteKnights.com.resturarent_system.security;

import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.OrderRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MessageChannel messageChannel;

    @InjectMocks
    private WebSocketAuthInterceptor interceptor;

    private User customerUser;
    private User staffUser;
    private Branch activeBranch;
    private Staff activeStaff;

    @BeforeEach
    void setUp() {
        Role customerRole = Role.builder().id(1L).name("CUSTOMER").build();
        customerUser = User.builder()
                .id(10L)
                .username("customer_test")
                .email("customer@test.com")
                .role(customerRole)
                .isActive(true)
                .build();

        Role staffRole = Role.builder().id(2L).name("RECEPTIONIST").build();
        staffUser = User.builder()
                .id(20L)
                .username("staff_test")
                .email("staff@test.com")
                .role(staffRole)
                .isActive(true)
                .build();

        activeBranch = Branch.builder()
                .id(1L)
                .name("Colombo Main")
                .status(BranchStatus.ACTIVE)
                .build();

        activeStaff = Staff.builder()
                .id(100L)
                .user(staffUser)
                .branch(activeBranch)
                .build();
    }

    private Message<?> createStompMessage(StompCommand command, String authHeader, String destination, Object userPrincipal) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setLeaveMutable(true);
        if (authHeader != null) {
            accessor.addNativeHeader("Authorization", authHeader);
        }
        if (destination != null) {
            accessor.setDestination(destination);
        }
        if (userPrincipal != null) {
            if (userPrincipal instanceof User u) {
                JwtUserPrincipal jwtPrincipal = new JwtUserPrincipal(u);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(jwtPrincipal, null, jwtPrincipal.getAuthorities());
                accessor.setUser(auth);
            }
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    // ──────────────────────── CONNECT TESTS ────────────────────────

    @Test
    void testConnect_ValidCustomerJwt_SetsAuthenticatedPrincipal() {
        String token = "valid.customer.jwt";
        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.getUserIdFromToken(token)).thenReturn(10L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(customerUser));

        Message<?> message = createStompMessage(StompCommand.CONNECT, "Bearer " + token, null, null);
        Message<?> result = interceptor.preSend(message, messageChannel);

        assertNotNull(result);
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
        assertNotNull(accessor.getUser());
        assertTrue(accessor.getUser() instanceof UsernamePasswordAuthenticationToken);

        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) accessor.getUser();
        JwtUserPrincipal principal = (JwtUserPrincipal) auth.getPrincipal();
        assertEquals(10L, principal.getUser().getId());
    }

    @Test
    void testConnect_ValidStaffJwt_SetsStaffPrincipal() {
        String token = "valid.staff.jwt";
        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.getUserIdFromToken(token)).thenReturn(20L);
        when(userRepository.findById(20L)).thenReturn(Optional.of(staffUser));
        when(staffRepository.findByUserIdWithBranch(20L)).thenReturn(Optional.of(activeStaff));

        Message<?> message = createStompMessage(StompCommand.CONNECT, "Bearer " + token, null, null);
        Message<?> result = interceptor.preSend(message, messageChannel);

        assertNotNull(result);
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
        assertNotNull(accessor.getUser());
    }

    @Test
    void testConnect_MissingAuthHeader_ThrowsException() {
        Message<?> message = createStompMessage(StompCommand.CONNECT, null, null, null);
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> interceptor.preSend(message, messageChannel));
    }

    @Test
    void testConnect_MalformedAuthHeader_ThrowsException() {
        Message<?> message = createStompMessage(StompCommand.CONNECT, "NotBearer token123", null, null);
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> interceptor.preSend(message, messageChannel));
    }

    @Test
    void testConnect_InvalidOrExpiredJwt_ThrowsException() {
        String token = "expired.jwt";
        when(jwtService.validateToken(token)).thenReturn(false);

        Message<?> message = createStompMessage(StompCommand.CONNECT, "Bearer " + token, null, null);
        assertThrows(BadCredentialsException.class,
                () -> interceptor.preSend(message, messageChannel));
    }

    @Test
    void testConnect_UserNotFound_ThrowsException() {
        String token = "valid.jwt";
        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.getUserIdFromToken(token)).thenReturn(999L);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        Message<?> message = createStompMessage(StompCommand.CONNECT, "Bearer " + token, null, null);
        assertThrows(BadCredentialsException.class,
                () -> interceptor.preSend(message, messageChannel));
    }

    @Test
    void testConnect_DeactivatedUser_ThrowsException() {
        customerUser.setIsActive(false);
        String token = "valid.jwt";
        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.getUserIdFromToken(token)).thenReturn(10L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(customerUser));

        Message<?> message = createStompMessage(StompCommand.CONNECT, "Bearer " + token, null, null);
        assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(message, messageChannel));
    }

    @Test
    void testConnect_StaffWithInactiveBranch_ThrowsException() {
        activeBranch.setStatus(BranchStatus.INACTIVE);
        String token = "valid.staff.jwt";
        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.getUserIdFromToken(token)).thenReturn(20L);
        when(userRepository.findById(20L)).thenReturn(Optional.of(staffUser));
        when(staffRepository.findByUserIdWithBranch(20L)).thenReturn(Optional.of(activeStaff));

        Message<?> message = createStompMessage(StompCommand.CONNECT, "Bearer " + token, null, null);
        assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(message, messageChannel));
    }

    // ──────────────────────── SUBSCRIBE TESTS ────────────────────────

    @Test
    void testSubscribe_OwnUserTopic_Allowed() {
        Message<?> message = createStompMessage(StompCommand.SUBSCRIBE, null, "/topic/user/10/orders", customerUser);
        Message<?> result = interceptor.preSend(message, messageChannel);
        assertNotNull(result);
    }

    @Test
    void testSubscribe_ForeignUserTopic_ThrowsAccessDenied() {
        Message<?> message = createStompMessage(StompCommand.SUBSCRIBE, null, "/topic/user/99/orders", customerUser);
        assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(message, messageChannel));
    }

    @Test
    void testSubscribe_CustomerAttemptsStaffTopic_ThrowsAccessDenied() {
        Message<?> message = createStompMessage(StompCommand.SUBSCRIBE, null, "/topic/branch/1/alerts", customerUser);
        assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(message, messageChannel));
    }

    @Test
    void testSubscribe_StaffOwnBranchTopic_Allowed() {
        when(staffRepository.findByUserIdWithBranch(20L)).thenReturn(Optional.of(activeStaff));

        Message<?> message = createStompMessage(StompCommand.SUBSCRIBE, null, "/topic/branch/1/kitchen-orders", staffUser);
        Message<?> result = interceptor.preSend(message, messageChannel);
        assertNotNull(result);
    }

    @Test
    void testSubscribe_StaffForeignBranchTopic_ThrowsAccessDenied() {
        when(staffRepository.findByUserIdWithBranch(20L)).thenReturn(Optional.of(activeStaff));

        Message<?> message = createStompMessage(StompCommand.SUBSCRIBE, null, "/topic/branch/2/kitchen-orders", staffUser);
        assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(message, messageChannel));
    }

    @Test
    void testSubscribe_OrderTopic_OwnerAllowed() {
        Customer customerEntity = Customer.builder().id(50L).user(customerUser).build();
        Order order = new Order();
        order.setId(105L);
        order.setCustomer(customerEntity);
        order.setBranch(activeBranch);
        when(orderRepository.findById(105L)).thenReturn(Optional.of(order));

        Message<?> message = createStompMessage(StompCommand.SUBSCRIBE, null, "/topic/order/105/status", customerUser);
        Message<?> result = interceptor.preSend(message, messageChannel);
        assertNotNull(result);
    }

    @Test
    void testSubscribe_OrderTopic_StrangerCustomerBlocked() {
        User otherUser = User.builder().id(99L).role(customerUser.getRole()).build();
        Customer customerEntity = Customer.builder().id(50L).user(otherUser).build();
        Order order = new Order();
        order.setId(105L);
        order.setCustomer(customerEntity);
        order.setBranch(activeBranch);
        when(orderRepository.findById(105L)).thenReturn(Optional.of(order));

        Message<?> message = createStompMessage(StompCommand.SUBSCRIBE, null, "/topic/order/105/status", customerUser);
        assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(message, messageChannel));
    }
}
