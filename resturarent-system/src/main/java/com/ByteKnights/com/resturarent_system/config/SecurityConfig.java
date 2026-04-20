package com.ByteKnights.com.resturarent_system.config;

import com.ByteKnights.com.resturarent_system.security.ForcePasswordChangeFilter;
import com.ByteKnights.com.resturarent_system.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/auth/staff/login").permitAll()
                .requestMatchers("/api/auth/change-password").authenticated()

                // Admin staff-management endpoints
                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

                // Role-management endpoints
                // SUPER_ADMIN only for changes
                .requestMatchers("/api/roles/create").hasRole("SUPER_ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/roles/*/permissions")
                    .hasRole("SUPER_ADMIN")

                // SUPER_ADMIN and ADMIN can view role privileges
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/roles/*/permissions")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN")

                // General staff-only test endpoints
                .requestMatchers("/api/staff/**")
                    .hasAnyRole("ADMIN", "MANAGER", "CHEF", "RECEPTIONIST", "DELIVERY", "SUPER_ADMIN")

                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            // JWT must run first to load authenticated user from token
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

            // Password-change enforcement runs after JWT auth is already set
            .addFilterAfter(forcePasswordChangeFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
}