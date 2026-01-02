package com.wellsfargo.signaturestudio.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA Configuration for Oracle database with Instant support.
 *
 * Key Configuration:
 * - Oracle12cDialect: Supports modern Oracle features including proper TIMESTAMP handling
 * - hibernate.jdbc.time_zone=UTC: Ensures all timestamps are stored/retrieved in UTC
 * - JDBC 4.2 (ojdbc11): Supports java.time.Instant natively
 * - InstantAttributeConverter: Explicit Instant → LocalDateTime converter
 *
 * This configuration ensures that:
 * 1. Instant values are properly converted to Oracle TIMESTAMP via LocalDateTime
 * 2. All timestamps are stored and compared in UTC
 * 3. Spring Data JPA method names work without manual CAST
 * 4. Queries like findByNextExpirTmstpAfter(Instant) work correctly
 *
 * IMPORTANT: Oracle's TIMESTAMP column is timezone-naive, but by setting
 * hibernate.jdbc.time_zone=UTC, we ensure consistent UTC storage and retrieval.
 *
 * The InstantAttributeConverter is manually applied to entity fields:
 * - @Convert(converter = InstantAttributeConverter.class) on each Instant field
 * - This provides explicit, controlled conversion only for database entities
 * - Does not affect DTOs, request/response objects, or service layer classes
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.wellsfargo.signaturestudio.repository")
public class JpaConfig {

    private static final Logger logger = LoggerFactory.getLogger(JpaConfig.class);

    @PostConstruct
    public void logJpaConfiguration() {
        logger.info("JPA Configuration initialized:");
        logger.info("  - Dialect: Oracle12cDialect");
        logger.info("  - Timezone: UTC");
        logger.info("  - Instant converter: InstantAttributeConverter (manual application)");
        logger.info("  - Converter scope: Explicitly applied to entity @Convert annotations only");
    }

    // Configuration is handled via application.properties:
    // - spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.Oracle12cDialect
    // - spring.jpa.properties.hibernate.jdbc.time_zone=UTC
}
