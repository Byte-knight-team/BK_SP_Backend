package com.ByteKnights.com.resturarent_system.security;

import com.ByteKnights.com.resturarent_system.entity.Staff;
import com.ByteKnights.com.resturarent_system.entity.User;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;

    public JwtAuthenticationFilter(JwtService jwtService,
            UserRepository userRepository,
            StaffRepository staffRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.staffRepository = staffRepository;
    }

    /*
     * Runs before every protected backend request.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // Read JWT token from Authorization header.
            String jwt = getJwtFromRequest(request);

            // Continue only if token exists and token is valid.
            if (StringUtils.hasText(jwt) && jwtService.validateToken(jwt)) {
                Long userId = jwtService.getUserIdFromToken(jwt);

                // Load the latest user data from database.
                User userEntity = userRepository.findById(userId).orElse(null);

                //If the user no longer exists, block the request, prevents old JWT tokens from working for missing or deleted users
                if (userEntity == null) {
                    denyRequest(
                            response,
                            "USER_NOT_FOUND",
                            "User account no longer exists");
                    return;
                }

                //If the user was deactivated after login, block the old JWT session.
                if (!Boolean.TRUE.equals(userEntity.getIsActive())) {
                    denyRequest(
                            response,
                            "USER_INACTIVE",
                            "Your account has been deactivated. Please contact the system administrator.");
                    return;
                }

                // If user exists and is active, continue with role and branch checks.
                String roleName = userEntity.getRole() != null
                        ? userEntity.getRole().getName()
                        : null;

                //SUPER_ADMIN is global, so branch status checking is skipped.
                boolean isSuperAdmin = "SUPER_ADMIN".equals(roleName) ||
                        "ROLE_SUPER_ADMIN".equals(roleName);

                if (!isSuperAdmin) {
        
                    // CUSTOMER users do NOT have Staff records.
                   boolean isCustomer = "CUSTOMER".equalsIgnoreCase(roleName)
                           || "ROLE_CUSTOMER".equalsIgnoreCase(roleName);
                   
                   if (!isSuperAdmin && !isCustomer) {
                          
                        //findByUserIdWithBranch uses JOIN FETCH and loads Staff + Branch together. 
                       Staff staff = staffRepository
                               .findByUserIdWithBranch(userEntity.getId())
                               .orElse(null);
                   
                       // If staff has no branch, block the request.
                       if (staff == null || staff.getBranch() == null) {
                           denyRequest(
                                   response,
                                   "STAFF_BRANCH_NOT_ASSIGNED",
                                   "Staff branch is not assigned");
                           return;
                       }
                   
                        //If staff branch is inactive, block the request. 
                       if (!"ACTIVE".equals(String.valueOf(staff.getBranch().getStatus()))) {
                           denyRequest(
                                   response,
                                   "BRANCH_INACTIVE",
                                   "Your branch is inactive. Please contact the system administrator.");
                           return;
                       }
                   }
                }

                // If all checks passed, create JwtUserPrincipal
                JwtUserPrincipal userDetails = new JwtUserPrincipal(userEntity);


                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

        } catch (Exception ex) {
            /*
            * If JWT checking fails, continue without logging in the user, will block protected endpoints later.
            */
            logger.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    /*
     * Sends a 403 Forbidden response manually.
     * Frontend can read the code value and logout the user automatically
     */
    private void denyRequest(HttpServletResponse response, String code, String message) throws IOException {
        SecurityContextHolder.clearContext();

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");

        response.getWriter().write(
                "{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }

    /*
     * Extract JWT token from Authorization header and remove Bearer and return only the token
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}