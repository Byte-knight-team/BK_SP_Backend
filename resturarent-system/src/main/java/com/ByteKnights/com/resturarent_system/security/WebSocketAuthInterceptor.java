package com.ByteKnights.com.resturarent_system.security;

import com.ByteKnights.com.resturarent_system.entity.Order;
import com.ByteKnights.com.resturarent_system.entity.Staff;
import com.ByteKnights.com.resturarent_system.entity.User;
import com.ByteKnights.com.resturarent_system.repository.OrderRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Principal;

/**
 * Intercepts incoming STOMP messages on the inbound channel to enforce
 * zero-trust JWT authentication on CONNECT and topic-level authorization on SUBSCRIBE.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final OrderRepository orderRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            handleSubscribe(accessor);
        }

        return message;
    }

    /**
     * Authenticates the client during the STOMP CONNECT frame.
     */
    private void handleConnect(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            log.warn("[WebSocket Security] Rejected CONNECT: Missing or malformed Authorization header.");
            throw new AuthenticationCredentialsNotFoundException("Missing or malformed Authorization header");
        }

        String token = authHeader.substring(7).trim();

        if (!jwtService.validateToken(token)) {
            log.warn("[WebSocket Security] Rejected CONNECT: Invalid or expired token.");
            throw new BadCredentialsException("Invalid or expired JWT token");
        }

        Long userId = jwtService.getUserIdFromToken(token);
        if (userId == null) {
            log.warn("[WebSocket Security] Rejected CONNECT: Token contains no userId claim.");
            throw new BadCredentialsException("Token missing user identity");
        }

        User userEntity = userRepository.findById(userId).orElse(null);
        if (userEntity == null) {
            log.warn("[WebSocket Security] Rejected CONNECT: User ID {} not found in database.", userId);
            throw new BadCredentialsException("User account does not exist");
        }

        if (!Boolean.TRUE.equals(userEntity.getIsActive())) {
            log.warn("[WebSocket Security] Rejected CONNECT: User ID {} account is deactivated.", userId);
            throw new AccessDeniedException("User account has been deactivated");
        }

        String roleName = userEntity.getRole() != null ? userEntity.getRole().getName() : null;
        boolean isSuperAdmin = "SUPER_ADMIN".equalsIgnoreCase(roleName) || "ROLE_SUPER_ADMIN".equalsIgnoreCase(roleName);
        boolean isCustomer = "CUSTOMER".equalsIgnoreCase(roleName) || "ROLE_CUSTOMER".equalsIgnoreCase(roleName);

        // Staff branch validation (matches JwtAuthenticationFilter)
        if (!isSuperAdmin && !isCustomer) {
            Staff staff = staffRepository.findByUserIdWithBranch(userEntity.getId()).orElse(null);
            if (staff == null || staff.getBranch() == null) {
                log.warn("[WebSocket Security] Rejected CONNECT: Staff ID {} has no assigned branch.", userEntity.getId());
                throw new AccessDeniedException("Staff branch is not assigned");
            }

            if (!"ACTIVE".equalsIgnoreCase(String.valueOf(staff.getBranch().getStatus()))) {
                log.warn("[WebSocket Security] Rejected CONNECT: Staff ID {} branch is inactive.", userEntity.getId());
                throw new AccessDeniedException("Staff branch is inactive");
            }
        }

        JwtUserPrincipal userDetails = new JwtUserPrincipal(userEntity);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        accessor.setUser(authentication);
        log.info("[WebSocket Security] Authenticated STOMP session: User ID={}, Role={}", userEntity.getId(), roleName);
    }

    /**
     * Enforces topic-level access controls during the STOMP SUBSCRIBE frame.
     */
    private void handleSubscribe(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        String destination = accessor.getDestination();

        if (destination == null) {
            return;
        }

        if (principal == null) {
            log.warn("[WebSocket Security] Rejected SUBSCRIBE to {}: Unauthenticated session.", destination);
            throw new AccessDeniedException("Unauthenticated subscription attempt");
        }

        User currentUser = extractUser(principal);
        if (currentUser == null) {
            log.warn("[WebSocket Security] Rejected SUBSCRIBE to {}: Invalid principal.", destination);
            throw new AccessDeniedException("Invalid authenticated principal");
        }

        String roleName = currentUser.getRole() != null ? currentUser.getRole().getName() : "";
        boolean isSuperAdmin = "SUPER_ADMIN".equalsIgnoreCase(roleName) || "ROLE_SUPER_ADMIN".equalsIgnoreCase(roleName);
        boolean isCustomer = "CUSTOMER".equalsIgnoreCase(roleName) || "ROLE_CUSTOMER".equalsIgnoreCase(roleName);

        // 1. User-specific private topics: /topic/user/{targetUserId}/**
        if (destination.startsWith("/topic/user/")) {
            String sub = destination.substring("/topic/user/".length());
            int slashIdx = sub.indexOf('/');
            String targetIdStr = slashIdx != -1 ? sub.substring(0, slashIdx) : sub;

            try {
                Long targetUserId = Long.parseLong(targetIdStr);
                if (!isSuperAdmin && !currentUser.getId().equals(targetUserId)) {
                    log.warn("[WebSocket Security] Blocked User ID {} from subscribing to foreign user topic {}",
                            currentUser.getId(), destination);
                    throw new AccessDeniedException("Unauthorized subscription to destination: " + destination);
                }
            } catch (NumberFormatException e) {
                log.warn("[WebSocket Security] Malformed user topic destination: {}", destination);
                throw new AccessDeniedException("Malformed destination: " + destination);
            }
        }

        // 2. Branch-specific staff topics: /topic/branch/{targetBranchId}/**
        else if (destination.startsWith("/topic/branch/")) {
            if (isCustomer) {
                log.warn("[WebSocket Security] Blocked Customer ID {} from subscribing to staff branch topic {}",
                        currentUser.getId(), destination);
                throw new AccessDeniedException("Customers are not permitted to subscribe to staff branch topics");
            }

            if (!isSuperAdmin) {
                String sub = destination.substring("/topic/branch/".length());
                int slashIdx = sub.indexOf('/');
                String targetBranchIdStr = slashIdx != -1 ? sub.substring(0, slashIdx) : sub;

                try {
                    Long targetBranchId = Long.parseLong(targetBranchIdStr);
                    Staff staff = staffRepository.findByUserIdWithBranch(currentUser.getId()).orElse(null);

                    if (staff == null || staff.getBranch() == null || !targetBranchId.equals(staff.getBranch().getId())) {
                        log.warn("[WebSocket Security] Blocked Staff ID {} from subscribing to cross-branch topic {}",
                                currentUser.getId(), destination);
                        throw new AccessDeniedException("Unauthorized subscription to branch: " + targetBranchId);
                    }
                } catch (NumberFormatException e) {
                    log.warn("[WebSocket Security] Malformed branch topic destination: {}", destination);
                    throw new AccessDeniedException("Malformed destination: " + destination);
                }
            }
        }

        // 3. Line-chef specific station topics: /topic/line-chef/{targetUserId}/**
        else if (destination.startsWith("/topic/line-chef/")) {
            String sub = destination.substring("/topic/line-chef/".length());
            int slashIdx = sub.indexOf('/');
            String targetIdStr = slashIdx != -1 ? sub.substring(0, slashIdx) : sub;

            try {
                Long targetUserId = Long.parseLong(targetIdStr);
                boolean isPrivileged = isSuperAdmin || "ADMIN".equalsIgnoreCase(roleName) || "CHEF".equalsIgnoreCase(roleName);

                if (!isPrivileged && !currentUser.getId().equals(targetUserId)) {
                    log.warn("[WebSocket Security] Blocked User ID {} from subscribing to line chef topic {}",
                            currentUser.getId(), destination);
                    throw new AccessDeniedException("Unauthorized subscription to line chef topic: " + destination);
                }
            } catch (NumberFormatException e) {
                throw new AccessDeniedException("Malformed destination: " + destination);
            }
        }

        // 4. Chef specific topics: /topic/chef/{targetUserId}/**
        else if (destination.startsWith("/topic/chef/")) {
            String sub = destination.substring("/topic/chef/".length());
            int slashIdx = sub.indexOf('/');
            String targetIdStr = slashIdx != -1 ? sub.substring(0, slashIdx) : sub;

            try {
                Long targetUserId = Long.parseLong(targetIdStr);
                if (!isSuperAdmin && !currentUser.getId().equals(targetUserId)) {
                    log.warn("[WebSocket Security] Blocked User ID {} from subscribing to chef topic {}",
                            currentUser.getId(), destination);
                    throw new AccessDeniedException("Unauthorized subscription to chef topic: " + destination);
                }
            } catch (NumberFormatException e) {
                throw new AccessDeniedException("Malformed destination: " + destination);
            }
        }

        // 5. Order confirmation status topic: /topic/order/{orderId}/status
        else if (destination.startsWith("/topic/order/")) {
            if (!isSuperAdmin) {
                String sub = destination.substring("/topic/order/".length());
                int slashIdx = sub.indexOf('/');
                String orderIdStr = slashIdx != -1 ? sub.substring(0, slashIdx) : sub;

                try {
                    Long orderId = Long.parseLong(orderIdStr);
                    Order order = orderRepository.findById(orderId).orElse(null);

                    if (order != null) {
                        boolean isOwner = order.getCustomer() != null &&
                                order.getCustomer().getUser() != null &&
                                currentUser.getId().equals(order.getCustomer().getUser().getId());

                        boolean isBranchStaff = false;
                        if (!isOwner && !isCustomer) {
                            Staff staff = staffRepository.findByUserIdWithBranch(currentUser.getId()).orElse(null);
                            if (staff != null && staff.getBranch() != null && order.getBranch() != null) {
                                isBranchStaff = staff.getBranch().getId().equals(order.getBranch().getId());
                            }
                        }

                        if (!isOwner && !isBranchStaff) {
                            log.warn("[WebSocket Security] Blocked User ID {} from subscribing to order #{}",
                                    currentUser.getId(), orderId);
                            throw new AccessDeniedException("Unauthorized subscription to order #" + orderId);
                        }
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    /**
     * Extracts the underlying User entity from Spring Security's Principal.
     */
    private User extractUser(Principal principal) {
        if (principal instanceof Authentication auth) {
            if (auth.getPrincipal() instanceof JwtUserPrincipal jwtPrincipal) {
                return jwtPrincipal.getUser();
            }
        }
        return null;
    }
}
