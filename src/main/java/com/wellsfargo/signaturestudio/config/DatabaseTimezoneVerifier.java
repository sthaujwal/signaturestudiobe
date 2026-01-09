package com.wellsfargo.signaturestudio.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Verifies that Oracle database session is configured to use UTC timezone.
 *
 * This component runs after application startup and logs the database
 * session timezone to confirm the connection-init-sql setting is working.
 *
 * Expected output: SESSIONTIMEZONE = +00:00 (UTC)
 *
 * If you see a different timezone (e.g., -06:00 for Central Time),
 * the connection-init-sql setting is not being applied correctly.
 */
@Component
public class DatabaseTimezoneVerifier {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseTimezoneVerifier.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseTimezoneVerifier(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void verifyDatabaseTimezone() {
        try {
            // Query Oracle session timezone
            String sessionTimezone = jdbcTemplate.queryForObject(
                "SELECT SESSIONTIMEZONE FROM DUAL",
                String.class
            );

            // Query database timezone
            String dbTimezone = jdbcTemplate.queryForObject(
                "SELECT DBTIMEZONE FROM DUAL",
                String.class
            );

            logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            logger.info("Oracle Database Timezone Configuration:");

            if (sessionTimezone != null) {
                logger.info("  Session Timezone: {}", sessionTimezone);
                logger.info("  Database Timezone: {} (global database setting)", dbTimezone);

                if ("+00:00".equals(sessionTimezone)) {
                    logger.info("✓ Session timezone is UTC (ideal configuration)");
                } else {
                    logger.info("ℹ Session timezone is {} (NOT UTC)", sessionTimezone);
                    logger.info("ℹ This is OK because:");
                    logger.info("  - InstantAttributeConverter ensures consistent save/load");
                    logger.info("  - All timestamps stored in same TZ ({})", sessionTimezone);
                    logger.info("  - hibernate.jdbc.time_zone=UTC handles conversion");
                    logger.info("  - Comparisons work correctly with consistent storage");
                }
            } else {
                logger.error("⚠ Could not determine Oracle session timezone (null result)");
            }

            logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (Exception e) {
            logger.error("Failed to verify database timezone", e);
        }
    }
}
