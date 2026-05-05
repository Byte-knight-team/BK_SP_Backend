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

    /*
     * Forces password change for users with passwordChanged == false
     * Runs for every request
     */
    @Component
    public class ForcePasswordChangeFilter extends OncePerRequestFilter {

        private final UserRepository userRepository;

        //This filter runs once per request
        public ForcePasswordChangeFilter(UserRepository userRepository) {
            this.userRepository = userRepository;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {

            String path = request.getRequestURI();

            //Login and change password endpoints are allowed
            boolean isLoginEndpoint = path.startsWith("/api/auth/staff/login");
            boolean isChangePasswordEndpoint = path.startsWith("/api/auth/staff/change-password");

            if (isLoginEndpoint || isChangePasswordEndpoint) {
                filterChain.doFilter(request, response);
                return;
            }

            //Get authentication from security context
            var auth = SecurityContextHolder.getContext().getAuthentication();

            if (!(auth instanceof UsernamePasswordAuthenticationToken authentication)) {
                filterChain.doFilter(request, response);
                //If there is no authenticated user, it allows the request to continue:
                return;
            }

            Object principal = authentication.getPrincipal();
            String email = null;

            //Get email from principal
            if (principal instanceof JwtUserPrincipal jwtUser) {
                email = jwtUser.getUsername();
            } else if (principal instanceof User user) {
                email = user.getEmail();
            }

            if (email != null) {
                //Load fresh user from database
                User freshUser = userRepository.findByEmail(email).orElse(null);

                if (freshUser != null) {

                    // Always enforce first password change for users with passwordChanged == false
                    if (!Boolean.TRUE.equals(freshUser.getPasswordChanged())) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.getWriter().write("""
                            {"success": false, "message": "You must change your temporary password before accessing this resource."}
                            """);
                        return;
                    }
                }
            }
            // Continue normally for users who already changed their password
            filterChain.doFilter(request, response);
        }
    }