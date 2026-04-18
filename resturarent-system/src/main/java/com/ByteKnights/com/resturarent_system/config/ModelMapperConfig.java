package com.ByteKnights.com.resturarent_system.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {
    /**
     * Registers ModelMapper as a Spring Bean.
     * This allows Spring to manage the ModelMapper instance and inject it
     * wherever it is needed using @Autowired.
     * * @return a new instance of ModelMapper
     */
    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

}
