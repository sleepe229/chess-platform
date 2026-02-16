package com.chess.analytics.config;

import org.springframework.boot.jpa.autoconfigure.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ensures Flyway migrations run before Hibernate EntityManagerFactory initialization.
 * This is critical for schema creation and table migrations.
 * Spring Boot auto-configures this dependency, but we make it explicit for clarity.
 */
@Configuration
public class FlywayConfig {

    @Bean
    public static EntityManagerFactoryDependsOnPostProcessor flywayEntityManagerFactoryDependsOnPostProcessor() {
        return new EntityManagerFactoryDependsOnPostProcessor("flywayInitializer");
    }
}
