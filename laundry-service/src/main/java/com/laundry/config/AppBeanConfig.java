package com.laundry.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

/**
 * Application-level Bean Configuration — Spring IoC
 *
 * Demonstrates explicit @Bean registration:
 *   - ObjectMapper (primary, shared across app)
 *   - RestTemplate (for inter-service calls in future)
 *
 * All beans defined here are singletons by default in the Spring IoC container.
 */
@Configuration
public class AppBeanConfig {

    /**
     * Primary ObjectMapper — registered as Spring IoC bean so it can be
     * injected wherever JSON serialization/deserialization is needed.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * RestTemplate bean — available for injection when inter-service HTTP calls
     * are needed (e.g., calling notification-service REST endpoints).
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
