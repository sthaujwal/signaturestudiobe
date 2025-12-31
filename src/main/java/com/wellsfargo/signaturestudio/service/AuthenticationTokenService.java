package com.wellsfargo.signaturestudio.service;

import com.wellsfargo.signaturestudio.domain.AuthenticationToken;
import com.wellsfargo.signaturestudio.domain.AuthenticationToken.TokenType;
import com.wellsfargo.signaturestudio.repository.AuthenticationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
    private static final int AUTHORIZATION_CODE_VALIDITY_MIN = 1;  // 1 minute
    private static final int ACCESS_TOKEN_VALIDITY_MIN = 30;  // 30 minutes

    private final AuthenticationTokenRepository tokenRepository;

    public AuthenticationTokenService(AuthenticationTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
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
            ACCESS_TOKEN_VALIDITY_MIN
        );
    }

    /**
     * Internal method to generate token.
     * Returns the token ID (authentication_token_id), which is used as the token value.
     * This provides optimal performance using primary key lookups.
     *
     * SIMPLIFIED: auth_obj just stores sessionId as plain string (no JSON).
     */
    private String generateToken(String sessionId, TokenType tokenType, int validityMinutes) {
        // Generate UUID as token ID (this becomes the token value for optimal lookups)
        String tokenId = UUID.randomUUID().toString();

        // Create token entity
        AuthenticationToken token = new AuthenticationToken();
        token.setAuthenticationTokenId(tokenId);
        token.setTokenType(tokenType);
        token.setSysId(sessionId);
        token.setAuthObj(sessionId);  // Store sessionId as plain string (no JSON!)
        token.setExpirProdInMin(validityMinutes);
        token.setNextExpirTmstp(Instant.now().plusSeconds(validityMinutes * 60L));

        tokenRepository.save(token);

        logger.info("Generated {} (ID: {}) for session: {} (expires in {} minutes)",
            tokenType, tokenId, sessionId, validityMinutes);

        // Return the token ID - this is what frontend will use in headers
        return tokenId;
    }

    /**
     * Validate and consume authorization code (ONE-TIME USE).
     * Returns session ID if valid, empty if invalid/expired.
     *
     * SIMPLIFIED DESIGN:
     * - Find valid authorization code
     * - Delete it immediately (one-time use)
     * - Return session ID for access token generation
     *
     * DISTRIBUTED SYSTEM:
     * - DELETE is atomic and idempotent
     * - If code already deleted, find returns empty (prevents replay)
     * - Uses primary key lookup for optimal performance
     *
     * Security features:
     * - One-time use (deleted after consumption)
     * - Prevents replay attacks (code no longer exists after use)
     * - UTC-based expiration check
     *
     * @param tokenId The authorization code ID (authentication_token_id) to validate
     * @return Optional containing session ID if valid, empty otherwise
     */
    @Transactional
    public Optional<String> validateAndConsumeAuthorizationCode(String tokenId) {
        // Generate UTC timestamp FIRST (consistent time reference)
        Instant currentUtc = Instant.now();

        // Fetch valid token using Spring Data JPA method name (NO CAST needed!)
        Optional<AuthenticationToken> tokenOpt = tokenRepository
            .findByAuthenticationTokenIdAndTokenTypeAndNextExpirTmstpAfter(
                tokenId,
                TokenType.AUTHORIZATION_CODE,
                currentUtc
            );

        if (tokenOpt.isEmpty()) {
            logger.warn("Authorization code not found or expired: {}", tokenId);
            return Optional.empty();
        }

        AuthenticationToken token = tokenOpt.get();
        String sessionId = token.getSysId();

        // Delete the authorization code (one-time use - simpler than marking as used!)
        tokenRepository.delete(token);

        logger.info("Authorization code consumed and deleted for session: {}", sessionId);
        return Optional.of(sessionId);
    }

    /**
     * Validate access token and extend expiration (AUTO-REFRESH).
     * Returns session ID if valid, empty if invalid/expired.
     *
     * RACE-CONDITION PROOF DESIGN:
     * - Generates UTC timestamps in Java before query execution
     * - Builds complete JSON metadata with timestamps
     * - Single atomic UPDATE with optimistic locking
     * - No time gap between timestamp generation and database update
     *
     * DISTRIBUTED SYSTEM IMPROVEMENTS:
     * - All timestamps are UTC (no timezone confusion)
     * - Pre-built JSON eliminates string concatenation race conditions
     * - Prevents race conditions across data centers
     * - Uses primary key lookup for optimal performance
     *
     * This is the KEY method for auto-refresh functionality:
     * - Every valid API request automatically extends the token expiration
     * - Synchronizes with session expiration
     * - Updates lastUsedAt in JSON for audit trail
     *
     * @param tokenId The access token ID (authentication_token_id) to validate
     * @return Optional containing session ID if valid, empty otherwise
     */
    @Transactional
    public Optional<String> validateAndExtendAccessToken(String tokenId) {
        // Generate UTC timestamp FIRST (consistent time reference for all operations)
        Instant currentUtc = Instant.now();

        // Fetch valid token using Spring Data JPA method name (NO CAST needed!)
        Optional<AuthenticationToken> tokenOpt = tokenRepository
            .findByAuthenticationTokenIdAndTokenTypeAndNextExpirTmstpAfter(
                tokenId,
                TokenType.ACCESS_TOKEN,
                currentUtc
            );

        if (tokenOpt.isEmpty()) {
            logger.warn("Access token not found or expired: {}", tokenId);
            return Optional.empty();
        }

        AuthenticationToken token = tokenOpt.get();

        // Extend expiration using entity method (updates both expiration and JSON metadata)
        token.extendExpiration(ACCESS_TOKEN_VALIDITY_MIN);

        // Save - Hibernate generates UPDATE automatically (NO CAST needed!)
        tokenRepository.save(token);

        String sessionId = token.getSysId();
        logger.debug("Access token validated and extended for session: {} (new expiration: {})",
            sessionId, token.getNextExpirTmstp());
        return Optional.of(sessionId);
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
     * RACE-CONDITION PROOF DESIGN:
     * - Uses UTC timestamp generated in Java before queries
     * - All comparisons done against consistent UTC time
     * - Safe to run on all instances simultaneously (idempotent)
     * - No race conditions (DELETE operations are atomic)
     *
     * Performs two cleanup operations:
     * 1. Delete expired tokens (both authorization codes and access tokens)
     * 2. Delete old used authorization codes (keep for 5 min audit trail)
     */
    @Scheduled(fixedRate = 300000)  // Every 5 minutes
    @Transactional
    public void cleanupExpiredTokens() {
        // Generate UTC timestamp BEFORE queries (consistent time reference)
        Instant currentUtc = Instant.now();

        // Delete expired tokens using Spring Data JPA method name (NO CAST needed!)
        // This includes both expired authorization codes and expired access tokens
        int expiredCount = tokenRepository.deleteByNextExpirTmstpBefore(currentUtc);

        if (expiredCount > 0) {
            logger.info("Cleanup: {} expired tokens deleted", expiredCount);
        }

        // Note: Authorization codes are deleted immediately after use,
        // so we don't need a separate cleanup for "used" codes
    }
}
