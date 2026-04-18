package com.ByteKnights.com.resturarent_system.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI restaurantSystemOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Restaurant System API")
                        .description("API documentation for BK_SP_Backend restaurant system")
                        .version("v1")
                        .contact(new Contact()
                                .name("ByteKnights Team")
                                .email("support@byteknights.local"))
                        .license(new License()
                                .name("Internal Use")
                                .url("https://example.com/license")));
    }
}
