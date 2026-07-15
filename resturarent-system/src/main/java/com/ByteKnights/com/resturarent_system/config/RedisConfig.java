package com.ByteKnights.com.resturarent_system.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
public class RedisConfig {

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Critical for parsing Java 8 LocalDateTime properly in JSON
        objectMapper.registerModule(new JavaTimeModule());
        
        // Ensure Jackson embeds the class name in the JSON so it can deserialize back into the correct DTO
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();
        objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)) // Aggressive 30-minute expiration
                .disableCachingNullValues() // Don't waste free-tier space on nulls
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
    }

    /*
     * Per-cache TTL overrides.
     *
     * Caches with lower mutation frequency get longer TTLs to maximize
     * cache hit rates. The default TTL (30 min) applies to all caches
     * not explicitly listed here.
     */
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return builder -> builder
                .withCacheConfiguration("crave:menu:categories",
                        cacheConfiguration().entryTtl(Duration.ofMinutes(60)))
                .withCacheConfiguration("crave:menu:subcategories",
                        cacheConfiguration().entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration("crave:menu:customer",
                        cacheConfiguration().entryTtl(Duration.ofMinutes(30)));
    }
}
