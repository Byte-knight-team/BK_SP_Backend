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
     * 4. Block request if user account is deactivated.
     * 5. Block request if branch-linked staff belongs to an inactive branch.
     * 6. If all checks pass, tell Spring Security that this user is authenticated.
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

                // Extract user ID stored inside JWT.
                Long userId = jwtService.getUserIdFromToken(jwt);

                /*
                 * Load the latest user data from database.
                 * This is important because the user may have been deactivated after login.
                 */
                User userEntity = userRepository.findById(userId).orElse(null);

                /*
                 * If the user no longer exists, block the request.
                 * This prevents old JWT tokens from working for deleted/missing users.
                 */
                if (userEntity == null) {
                    denyRequest(
                            response,
                            "USER_NOT_FOUND",
                            "User account no longer exists");
                    return;
                }

                /*
                 * If the user was deactivated after login, block the old JWT session.
                 * This handles already logged-in users after staff/user deactivation.
                 */
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

                /*
                 * SUPER_ADMIN is global, so branch status checking is skipped.
                 * This supports both role name formats:
                 * - SUPER_ADMIN
                 * - ROLE_SUPER_ADMIN
                 */
                boolean isSuperAdmin = "SUPER_ADMIN".equals(roleName) ||
                        "ROLE_SUPER_ADMIN".equals(roleName);

                /*
                 * Non-SUPER_ADMIN users must have a Staff record and an assigned branch.
                 * Their branch must also be ACTIVE.
                 */
                if (!isSuperAdmin) {
                    /*
                    * CUSTOMER users do NOT have Staff records.
                    * Only staff-side roles such as ADMIN, MANAGER, CHEF, RECEPTIONIST,
                    * and DELIVERY need branch validation.
                    */
                   boolean isCustomer = "CUSTOMER".equalsIgnoreCase(roleName)
                           || "ROLE_CUSTOMER".equalsIgnoreCase(roleName);
                   
                   if (!isSuperAdmin && !isCustomer) {
                   
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
                                   "Staff branch is not assigned");
                           return;
                       }
                   
                       /*
                        * If staff branch is inactive, block the request.
                        * This handles already logged-in staff after their branch is deactivated.
                        */
                       if (!"ACTIVE".equals(String.valueOf(staff.getBranch().getStatus()))) {
                           denyRequest(
                                   response,
                                   "BRANCH_INACTIVE",
                                   "Your branch is inactive. Please contact the system administrator.");
                           return;
                       }
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
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());

                // Attach request details like IP/session info.
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                /*
                 * Save authentication in Spring Security context.
                 * This means the request is now treated as authenticated.
                 */
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

        } catch (Exception ex) {
            /*
             * If token validation or user loading fails, do not crash the backend.
             * The request will continue without authentication and Spring Security will
             * block it later.
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
     * - User account no longer exists.
     * - User account is inactive.
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
                "{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
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