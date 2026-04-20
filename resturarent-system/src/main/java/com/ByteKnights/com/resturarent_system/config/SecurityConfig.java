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
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/auth/staff/login").permitAll()
                .requestMatchers("/api/auth/change-password").authenticated()

                // Admin staff-management endpoints
                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

                // Role management endpoints
                .requestMatchers(HttpMethod.POST, "/api/roles/create").hasRole("SUPER_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/roles/*").hasRole("SUPER_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/roles/*").hasRole("SUPER_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/roles/*/permissions").hasRole("SUPER_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/roles").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/roles/*/summary").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/roles/*/permissions").hasAnyRole("SUPER_ADMIN", "ADMIN")

                // Privileges endpoint
                .requestMatchers(HttpMethod.GET, "/api/privileges").hasAnyRole("SUPER_ADMIN", "ADMIN")

                // Email testing endpoint
                .requestMatchers("/api/email-testing/**").hasRole("SUPER_ADMIN")

                // General staff endpoints
                .requestMatchers("/api/staff/**")
                .hasAnyRole("ADMIN", "MANAGER", "CHEF", "RECEPTIONIST", "DELIVERY", "SUPER_ADMIN")

                // Everything else
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(forcePasswordChangeFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
}