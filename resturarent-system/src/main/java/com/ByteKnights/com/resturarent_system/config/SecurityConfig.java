package com.ByteKnights.com.resturarent_system.config;

import com.ByteKnights.com.resturarent_system.security.ForcePasswordChangeFilter;
import com.ByteKnights.com.resturarent_system.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ForcePasswordChangeFilter forcePasswordChangeFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
            ForcePasswordChangeFilter forcePasswordChangeFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.forcePasswordChangeFilter = forcePasswordChangeFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(auth -> auth

                        /*
                         * Public endpoints.
                         * These endpoints do not require JWT authentication.
                         */
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/qr-sessions/**",
                                "/api/v1/menu/customer",
                                "/api/v1/auth/customer",
                                "/api/tables",
                                "/api/tables/**",
                                "/api/auth/staff/login",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api-docs/**",
                                "/v3/api-docs/**")
                        .permitAll()

                        /*
                         * Staff password change requires authenticated staff user.
                         */
                        .requestMatchers(HttpMethod.PUT, "/api/auth/staff/change-password")
                        .authenticated()

                        /*
                         * Customer operations.
                         */
                        .requestMatchers("/api/v1/customer/**")
                        .hasRole("CUSTOMER")

                        /*
                         * Branch management.
                         * Only SUPER_ADMIN can create/update/deactivate branches.
                         */
                        .requestMatchers("/api/admin/branches", "/api/admin/branches/**")
                        .hasRole("SUPER_ADMIN")

                        /*
                         * Staff management.
                         * SUPER_ADMIN and ADMIN can access staff endpoints.
                         *
                         * Actual branch-level restrictions are handled inside StaffService.
                         * Example:
                         * - ADMIN can manage only own-branch staff.
                         * - ADMIN cannot manage SUPER_ADMIN or ADMIN-level accounts.
                         */
                        .requestMatchers("/api/admin/staff", "/api/admin/staff/**")
                        .hasAnyRole("SUPER_ADMIN", "ADMIN")

                        /*
                         * Role READ access.
                         *
                         * SUPER_ADMIN needs this for Roles & Permissions.
                         * ADMIN needs this only for Create Staff / Edit Staff role dropdown.
                         *
                         * This does NOT allow ADMIN to create, update, delete,
                         * or change role permissions.
                         */
                        .requestMatchers(HttpMethod.GET, "/api/admin/roles")
                        .hasAnyRole("SUPER_ADMIN", "ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/admin/roles/*")
                        .hasAnyRole("SUPER_ADMIN", "ADMIN")

                        /*
                         * Role permission READ access.
                         *
                         * Keep this SUPER_ADMIN only because this is part of RBAC management.
                         * Create Staff / Edit Staff does not need this endpoint
                         */
                        .requestMatchers(HttpMethod.GET, "/api/admin/roles/*/permissions")
                        .hasRole("SUPER_ADMIN")

                        /*
                         * Role MANAGEMENT access.
                         * Only SUPER_ADMIN can create roles, update roles,
                         * update permissions, or delete roles.
                         */
                        .requestMatchers(HttpMethod.POST, "/api/admin/roles")
                        .hasRole("SUPER_ADMIN")

                        .requestMatchers(HttpMethod.PUT, "/api/admin/roles/*")
                        .hasRole("SUPER_ADMIN")

                        .requestMatchers(HttpMethod.PUT, "/api/admin/roles/*/permissions")
                        .hasRole("SUPER_ADMIN")

                        .requestMatchers(HttpMethod.DELETE, "/api/admin/roles/*")
                        .hasRole("SUPER_ADMIN")

                        /*
                         * Privileges list.
                         * Keep SUPER_ADMIN only because privileges are part of RBAC management.
                         */
                        .requestMatchers(HttpMethod.GET, "/api/admin/privileges")
                        .hasRole("SUPER_ADMIN")

                        /*
                         * Email testing.
                         */
                        .requestMatchers("/api/email-testing/**")
                        .hasRole("SUPER_ADMIN")

                        /*
                         * Fallback for other admin endpoints.
                         */
                        .requestMatchers("/api/admin/**")
                        .hasAnyRole("SUPER_ADMIN", "ADMIN")

                        /*
                         * Everything else needs authentication.
                         */
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(forcePasswordChangeFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
}
