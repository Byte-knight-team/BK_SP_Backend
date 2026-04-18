package com.ByteKnights.com.resturarent_system.security;

import com.ByteKnights.com.resturarent_system.entity.User;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ForcePasswordChangeFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public ForcePasswordChangeFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        // Allow login and password change endpoints
        if (requestPath.equals("/api/auth/staff/login") ||
            requestPath.equals("/api/auth/change-password")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() instanceof UsernamePasswordAuthenticationToken authentication) {

            Object principal = authentication.getPrincipal();
            String email = null;

            // Determine email from principal
            if (principal instanceof User user) {
                email = user.getEmail();
            } else if (principal instanceof org.springframework.security.core.userdetails.User springUser) {
                email = springUser.getUsername();
            }

            if (email != null) {
                User freshUser = userRepository.findByEmail(email).orElse(null);
                if (freshUser != null) {

                    // Skip admin and super-admin
                    String roleName = freshUser.getRole().getName();
                    if ("ADMIN".equals(roleName) || "SUPER_ADMIN".equals(roleName)) {
                        filterChain.doFilter(request, response);
                        return;
                    }

                    // Enforce force-password-change for all other staff
                    if (Boolean.FALSE.equals(freshUser.getPasswordChanged())) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.getWriter().write("""
                            {
                              "success": false,
                              "message": "You must change your temporary password before accessing this resource."
                            }
                            """);
                        return;
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}