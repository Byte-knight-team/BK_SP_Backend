package com.ByteKnights.com.resturarent_system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                // React dev server (Vite default)
                .allowedOrigins("http://localhost:5173")
                // Allow standard HTTP methods
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                // Allow all headers (including Authorization for future JWT)
                .allowedHeaders("*")
                // Expose Authorization header to the frontend
                .exposedHeaders("Authorization")
                // Allow cookies/credentials if needed later
                .allowCredentials(true)
                // Cache preflight response for 1 hour (reduces OPTIONS overhead)
                .maxAge(3600);
    }
}
