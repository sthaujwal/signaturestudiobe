package com.wellsfargo.signaturestudio.service;

import com.wellsfargo.signaturestudio.domain.AuthenticationToken;
import com.wellsfargo.signaturestudio.domain.AuthenticationToken.TokenType;
import com.wellsfargo.signaturestudio.repository.AuthenticationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Unified service for managing both authorization codes and access tokens.
 * Uses a single table with token_type discrimination and JSON metadata in CLOB.
 *
 * Authorization Code Flow:
 * 1. generateAuthorizationCode() - Create short-lived code after Ping IdP auth
 * 2. validateAndConsumeAuthorizationCode() - Exchange code for access token (one-time use)
 * 3. generateAccessToken() - Create long-lived token for API access
 * 4. validateAndExtendAccessToken() - Validate and auto-extend on each API call
 * 5. revokeTokensForSession() - Clean up on logout
 */
@Service
public class AuthenticationTokenService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationTokenService.class);

    // Configuration constants
    private static final int AUTHORIZATION_CODE_LENGTH = 32;  // bytes
    private static final int AUTHORIZATION_CODE_VALIDITY_MIN = 1;  // 1 minute

    private static final int ACCESS_TOKEN_LENGTH = 48;  // bytes
    private static final int ACCESS_TOKEN_VALIDITY_MIN = 30;  // 30 minutes

    private final AuthenticationTokenRepository tokenRepository;
    private final SecureRandom secureRandom;

    public AuthenticationTokenService(AuthenticationTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
        this.secureRandom = new SecureRandom();
    }

    /**
     * Generate short-lived authorization code (60 seconds, one-time use).
     * Used in redirect URL after Ping IdP authentication.
     *
     * @param sessionId The session ID to associate with the code
     * @return The generated authorization code value
     */
    public String generateAuthorizationCode(String sessionId) {
        return generateToken(
            sessionId,
            TokenType.AUTHORIZATION_CODE,
            AUTHORIZATION_CODE_LENGTH,
            AUTHORIZATION_CODE_VALIDITY_MIN
        );
    }

    /**
     * Generate long-lived access token (30 minutes, reusable, auto-extends).
     * Used in X-SignatureStudio-Token header for API calls.
     *
     * @param sessionId The session ID to associate with the token
     * @return The generated access token value
     */
    public String generateAccessToken(String sessionId) {
        return generateToken(
            sessionId,
            TokenType.ACCESS_TOKEN,
            ACCESS_TOKEN_LENGTH,
            ACCESS_TOKEN_VALIDITY_MIN
        );
    }

    /**
     * Internal method to generate token with JSON metadata in CLOB.
     */
    private String generateToken(String sessionId, TokenType tokenType, int lengthBytes, int validityMinutes) {
        // Generate cryptographically secure random token
        byte[] randomBytes = new byte[lengthBytes];
        secureRandom.nextBytes(randomBytes);
        String tokenValue = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        // Create token entity with JSON metadata
        AuthenticationToken token = new AuthenticationToken();
        token.setAuthenticationTokenId(UUID.randomUUID().toString());
        token.setTokenType(tokenType);
        token.setSysId(sessionId);
        token.setExpirProdInMin(validityMinutes);
        token.setNextExpirTmstp(Instant.now().plusSeconds(validityMinutes * 60L));

        // Set token value in metadata (will be serialized to JSON in auth_obj CLOB)
        token.setTokenValue(tokenValue);

        tokenRepository.save(token);

        logger.info("Generated {} for session: {} (expires in {} minutes)",
            tokenType, sessionId, validityMinutes);

        return tokenValue;
    }

    /**
     * Validate and consume authorization code (ONE-TIME USE).
     * Returns session ID if valid, empty if invalid/expired/already used.
     *
     * DISTRIBUTED SYSTEM IMPROVEMENTS:
     * - Uses atomic database operation with JSON_TRANSFORM to mark code as used
     * - Uses database timestamp for expiration check (eliminates clock skew)
     * - Prevents race conditions across multiple data centers
     * - Single database round-trip for better performance
     *
     * Security features:
     * - One-time use (marks as consumed atomically in JSON metadata)
     * - Prevents replay attacks across DCs
     * - Checks expiration using Oracle's clock
     * - Returns session ID for token generation
     *
     * @param codeValue The authorization code to validate
     * @return Optional containing session ID if valid, empty otherwise
     */
    @Transactional
    public Optional<String> validateAndConsumeAuthorizationCode(String codeValue) {
        // Atomically mark code as used in JSON metadata (prevents race conditions across DCs)
        // Returns 1 if successful, 0 if already used/expired/not found
        int updated = tokenRepository.markAuthorizationCodeAsUsed(codeValue);

        if (updated == 0) {
            logger.warn("Authorization code invalid, already used, or expired");
            return Optional.empty();
        }

        // Fetch session ID (code is now marked as used in JSON)
        Optional<AuthenticationToken> tokenOpt = tokenRepository.findValidTokenByValue(codeValue);

        if (tokenOpt.isPresent()) {
            String sessionId = tokenOpt.get().getSysId();
            logger.info("Authorization code consumed for session: {}", sessionId);
            return Optional.of(sessionId);
        }

        // Should not happen (we just updated it), but handle gracefully
        logger.error("Authorization code marked as used but not found in database: {}", codeValue);
        return Optional.empty();
    }

    /**
     * Validate access token and extend expiration (AUTO-REFRESH).
     * Returns session ID if valid, empty if invalid/expired.
     *
     * DISTRIBUTED SYSTEM IMPROVEMENTS:
     * - Uses atomic database operation with JSON_TRANSFORM to extend token
     * - Uses database timestamp for expiration (eliminates clock skew)
     * - Prevents race conditions across multiple data centers
     * - Single database UPDATE for better performance
     * - Updates lastUsedAt in JSON metadata for audit trail
     *
     * This is the KEY method for auto-refresh functionality:
     * - Every valid API request automatically extends the token expiration
     * - Synchronizes with session expiration
     * - Updates lastUsedAt in JSON for audit trail
     *
     * @param tokenValue The access token to validate
     * @return Optional containing session ID if valid, empty otherwise
     */
    @Transactional
    public Optional<String> validateAndExtendAccessToken(String tokenValue) {
        // Atomically extend token expiration and update JSON metadata (prevents race conditions across DCs)
        // Returns 1 if successful, 0 if token not found/expired/wrong type
        int updated = tokenRepository.extendAccessTokenExpiration(
            tokenValue,
            ACCESS_TOKEN_VALIDITY_MIN
        );

        if (updated == 0) {
            logger.warn("Access token not found, expired, or invalid");
            return Optional.empty();
        }

        // Fetch session ID (token has been extended)
        // Use findValidTokenByValue() to double-check using database timestamp
        Optional<AuthenticationToken> tokenOpt = tokenRepository.findValidTokenByValue(tokenValue);

        if (tokenOpt.isPresent()) {
            String sessionId = tokenOpt.get().getSysId();
            logger.debug("Access token validated and extended for session: {}", sessionId);
            return Optional.of(sessionId);
        }

        // Should not happen (we just updated it), but handle gracefully
        logger.error("Access token extended but not found in database: {}", tokenValue);
        return Optional.empty();
    }

    /**
     * Revoke all tokens for a session (on logout).
     * Removes both authorization codes and access tokens.
     *
     * DISTRIBUTED SYSTEM: Safe for concurrent calls from multiple DCs.
     * - DELETE operation is idempotent (safe to call multiple times)
     * - If tokens already deleted, returns 0 (no error)
     * - Foreign key CASCADE ensures cleanup even if listener doesn't fire
     *
     * Called by:
     * - SessionEventListener when session is destroyed
     * - Explicit logout endpoint
     * - Database CASCADE when session deleted
     *
     * @param sessionId The session ID whose tokens should be revoked
     */
    @Transactional
    public void revokeTokensForSession(String sessionId) {
        int deleted = tokenRepository.deleteBySysId(sessionId);

        if (deleted > 0) {
            logger.info("Revoked {} token(s) for session: {}", deleted, sessionId);
        } else {
            // Already revoked (by another DC or CASCADE) - this is normal in multi-DC
            logger.debug("No tokens found for session (already revoked): {}", sessionId);
        }
    }

    /**
     * Scheduled cleanup of expired tokens.
     * Runs every 5 minutes to maintain database hygiene.
     *
     * DISTRIBUTED SYSTEM IMPROVEMENTS:
     * - Uses database timestamp for consistency across DCs
     * - Safe to run on all instances simultaneously (idempotent)
     * - No race conditions (DELETE operations are atomic)
     * - Works with JSON metadata in CLOB
     *
     * Performs two cleanup operations:
     * 1. Delete expired tokens (both authorization codes and access tokens)
     * 2. Delete old used authorization codes (keep for 5 min audit trail)
     */
    @Scheduled(fixedRate = 300000)  // Every 5 minutes
    @Transactional
    public void cleanupExpiredTokens() {
        // Delete expired tokens (uses database timestamp - no clock skew issues)
        int expiredCount = tokenRepository.deleteExpiredTokens();

        // Delete old used authorization codes (keep for 5 min audit trail)
        // This queries JSON metadata to find codes marked as used
        int usedCodesCount = tokenRepository.deleteOldUsedAuthorizationCodes(5);

        if (expiredCount > 0 || usedCodesCount > 0) {
            logger.info("Cleanup: {} expired tokens, {} old authorization codes",
                expiredCount, usedCodesCount);
        }
    }
}
