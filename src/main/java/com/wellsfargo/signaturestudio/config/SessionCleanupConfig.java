package com.wellsfargo.signaturestudio.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Session and token cleanup configuration.
 *
 * NEW APPROACH (Session Attributes):
 * ✅ Access tokens stored in SPRING_SESSION_ATTRIBUTES (not database)
 * ✅ Tokens live as long as session (no expiration, no refresh needed)
 * ✅ Spring Session automatically deletes session + attributes on expiration
 * ✅ Authorization codes in database (cleaned every 5 min)
 *
 * CLEANUP STRATEGY:
 * ✅ Spring Session JDBC deletes expired sessions + attributes (@EnableJdbcHttpSession)
 * ✅ AuthenticationTokenService.cleanupExpiredAuthorizationCodes() (every 5 min)
 * ✅ No access token cleanup needed (session attributes auto-deleted)
 *
 * BENEFITS:
 * - Simpler code (no token refresh logic)
 * - Faster validation (session attribute lookup, not database query)
 * - Automatic cleanup (Spring handles it)
 * - Token always synchronized with session
 */
@Configuration
@EnableScheduling
public class SessionCleanupConfig {

    private static final Logger logger = LoggerFactory.getLogger(SessionCleanupConfig.class);

    public SessionCleanupConfig() {
        logger.info("Session cleanup configuration initialized");
        logger.info("✓ Access tokens: Stored in session attributes (auto-cleanup)");
        logger.info("✓ Authorization codes: Cleaned every 5 minutes");
        logger.info("✓ Session expiration: Handled by Spring Session");
    }
}
