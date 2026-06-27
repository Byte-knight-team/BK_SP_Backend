package com.ByteKnights.com.resturarent_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication(exclude = { UserDetailsServiceAutoConfiguration.class })
@ConfigurationPropertiesScan
@EnableScheduling
public class ResturarentSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResturarentSystemApplication.class, args);
    }
}
