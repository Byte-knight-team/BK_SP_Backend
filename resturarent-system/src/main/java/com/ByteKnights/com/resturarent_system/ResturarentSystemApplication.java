package com.ByteKnights.com.resturarent_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = { UserDetailsServiceAutoConfiguration.class })
public class ResturarentSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResturarentSystemApplication.class, args);
    }
}
