package com.ByteKnights.com.resturarent_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication(exclude = { UserDetailsServiceAutoConfiguration.class })
@ConfigurationPropertiesScan
@EnableScheduling
@EnableCaching
public class ResturarentSystemApplication {

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Colombo"));
    }

    public static void main(String[] args) {
        SpringApplication.run(ResturarentSystemApplication.class, args);
    }
}
