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
     * This method runs before every protected backend request.
     *
     * Main job:
     * 1. Read JWT token from request header.
     * 2. Validate JWT token.
     * 3. Load logged-in user from database.
     * 4. Check whether user account is active.
     * 5. Check whether branch-linked staff belongs to an ACTIVE branch.
     * 6. If all checks pass, tell Spring Security that this user is authenticated.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            // Read JWT token from Authorization header
            String jwt = getJwtFromRequest(request);

            // Continue only if token exists and token is valid.
            if (StringUtils.hasText(jwt) && jwtService.validateToken(jwt)) {

                // Extract user ID stored inside JWT.
                Long userId = jwtService.getUserIdFromToken(jwt);

                // Load the latest user data from database.
                User userEntity = userRepository.findById(userId).orElse(null);

                // Only active users are allowed to continue.
                if (userEntity != null && Boolean.TRUE.equals(userEntity.getIsActive())) {

                    String roleName = userEntity.getRole() != null
                            ? userEntity.getRole().getName()
                            : null;

                    /*
                     * we skip branch-status checking for SUPER_ADMIN.
                     * Some projects store role as SUPER_ADMIN.
                     * Some store it as ROLE_SUPER_ADMIN.
                     */
                    boolean isSuperAdmin =
                            "SUPER_ADMIN".equals(roleName) ||
                            "ROLE_SUPER_ADMIN".equals(roleName);

                    /*

                    Non- super admin must have a Staff record and an assigned branch.*/

                     
                    if (!isSuperAdmin) {

                        /*
                         * Use findByUserIdWithBranch instead of findByUserId.
                         *
                         * Reason:
                         * Branch is lazy-loaded in Staff entity.
                         * If we use normal findByUserId, staff.getBranch().getStatus()
                         * can cause LazyInitializationException inside this filter.
                         *
                         * findByUserIdWithBranch uses JOIN FETCH and loads Staff + Branch together.
                         */
                        Staff staff = staffRepository
                                .findByUserIdWithBranch(userEntity.getId())
                                .orElse(null);

                        // If staff has no branch, block the request.
                        if (staff == null || staff.getBranch() == null) {
                            denyRequest(
                                    response,
                                    "STAFF_BRANCH_NOT_ASSIGNED",
                                    "Staff branch is not assigned"
                            );
                            return;
                        }

                        // If staff branch is inactive, block the request.
                        // This handles already logged-in users after their branch is deactivated.
                        if (!"ACTIVE".equals(String.valueOf(staff.getBranch().getStatus()))) {
                            denyRequest(
                                    response,
                                    "BRANCH_INACTIVE",
                                    "Your branch is inactive. Please contact the system administrator."
                            );
                            return;
                        }
                    }

                    /*
                     * If all checks passed, create JwtUserPrincipal.
                     * This object contains logged-in user details and authorities/permissions.
                     */
                    JwtUserPrincipal userDetails = new JwtUserPrincipal(userEntity);

                    /*
                     * Create Spring Security authentication object.
                     * After this, backend controllers can recognize the logged-in user.
                     */
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    // Attach request details like IP/session info.
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // Save authentication in Spring Security context.
                    // This means the request is now treated as authenticated.
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }

        } catch (Exception ex) {
            /*
             * If token validation or user loading fails, do not crash the backend.
             * The request will continue without authentication and Spring Security will block it later.
             */
            logger.error("Could not set user authentication in security context", ex);
        }

        // Continue to the next filter/controller.
        filterChain.doFilter(request, response);
    }

    /*
     * Sends a 403 Forbidden response manually.
     *
     * Used when:
     * - Staff branch is not assigned.
     * - Staff branch is inactive.
     *
     * Frontend can read the "code" value and logout the user automatically.
     */
    private void denyRequest(HttpServletResponse response, String code, String message) throws IOException {
        SecurityContextHolder.clearContext();

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");

        response.getWriter().write(
                "{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}"
        );
    }

    /*
     * Extract JWT token from Authorization header.
     *
     * Header format:
     * Authorization: Bearer <token>
     *
     * This method removes "Bearer " and returns only the token part.
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}