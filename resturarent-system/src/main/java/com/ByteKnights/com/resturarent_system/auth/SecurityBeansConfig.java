package com.ByteKnights.com.resturarent_system.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/*
    SecurityBeansConfig contains security-related beans
*/
@Configuration
public class SecurityBeansConfig {

    /*
     * Creates a PasswordEncoder bean, spring will manage this bean and inject it wherever password hashing or password verification is needed.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {

        /*
         * BCryptPasswordEncoder hashes passwords using the BCrypt algorithm.
         * Raw passwords are not stored directly in the database, only the hashed version is stored.
         */
        return new BCryptPasswordEncoder();
    }
}