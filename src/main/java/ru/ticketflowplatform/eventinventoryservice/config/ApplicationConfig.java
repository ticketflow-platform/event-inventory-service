package ru.ticketflowplatform.eventinventoryservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.ObjectMapper;

@Configuration
class ApplicationConfig {

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
