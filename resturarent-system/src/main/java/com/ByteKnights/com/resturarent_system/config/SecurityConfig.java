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
                // Public
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
                    "/v3/api-docs/**"
                ).permitAll()

                

                // Authenticated auth flow
                .requestMatchers(HttpMethod.PUT, "/api/auth/staff/change-password").authenticated()

                // customer opereations customer only
                .requestMatchers("/api/v1/customer/**").hasRole("CUSTOMER")

                // Branch management - SUPER_ADMIN only
                .requestMatchers("/api/admin/branches", "/api/admin/branches/**").hasRole("SUPER_ADMIN")

                // Staff management - SUPER_ADMIN and ADMIN
                .requestMatchers("/api/admin/staff", "/api/admin/staff/**")
                .hasAnyRole("SUPER_ADMIN", "ADMIN")

                // Role management
                .requestMatchers(HttpMethod.POST, "/api/admin/roles").hasRole("SUPER_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/admin/roles/*").hasRole("SUPER_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/admin/roles/*/permissions").hasRole("SUPER_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/admin/roles/*").hasRole("SUPER_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/admin/roles").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/admin/roles/*").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/admin/roles/*/permissions").hasAnyRole("SUPER_ADMIN", "ADMIN")

                // Privileges
                .requestMatchers(HttpMethod.GET, "/api/admin/privileges").hasAnyRole("SUPER_ADMIN", "ADMIN")

                // Email testing
                .requestMatchers("/api/email-testing/**").hasRole("SUPER_ADMIN")

                // Fallback for other admin endpoints
                .requestMatchers("/api/admin/**").hasAnyRole("SUPER_ADMIN", "ADMIN")

                // Everything else
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(forcePasswordChangeFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
}