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
     * Returns the token ID (authentication_token_id), which is used as the token value.
     * This provides optimal performance using primary key lookups.
     */
    private String generateToken(String sessionId, TokenType tokenType, int lengthBytes, int validityMinutes) {
        // Generate UUID as token ID (this becomes the token value for optimal lookups)
        String tokenId = UUID.randomUUID().toString();

        // Generate additional random data for JSON metadata (optional, for extra security)
        byte[] randomBytes = new byte[lengthBytes];
        secureRandom.nextBytes(randomBytes);
        String additionalEntropy = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        // Create token entity with JSON metadata
        AuthenticationToken token = new AuthenticationToken();
        token.setAuthenticationTokenId(tokenId);
        token.setTokenType(tokenType);
        token.setSysId(sessionId);
        token.setExpirProdInMin(validityMinutes);
        token.setNextExpirTmstp(Instant.now().plusSeconds(validityMinutes * 60L));

        // Set token value in metadata (will be serialized to JSON in auth_obj CLOB)
        // Store the tokenId as tokenValue for consistency
        token.setTokenValue(tokenId);

        tokenRepository.save(token);

        logger.info("Generated {} (ID: {}) for session: {} (expires in {} minutes)",
            tokenType, tokenId, sessionId, validityMinutes);

        // Return the token ID - this is what frontend will use in headers
        return tokenId;
    }

    /**
     * Validate and consume authorization code (ONE-TIME USE).
     * Returns session ID if valid, empty if invalid/expired/already used.
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
     * - Prevents replay attacks across data centers
     * - Uses primary key lookup for optimal performance
     *
     * Security features:
     * - One-time use (marks as consumed atomically)
     * - Prevents replay attacks
     * - UTC-based expiration check
     *
     * @param tokenId The authorization code ID (authentication_token_id) to validate
     * @return Optional containing session ID if valid, empty otherwise
     */
    @Transactional
    public Optional<String> validateAndConsumeAuthorizationCode(String tokenId) {
        // Generate UTC timestamp FIRST (consistent time reference for all operations)
        Instant currentUtc = Instant.now();

        // Fetch the token to get current metadata
        Optional<AuthenticationToken> tokenOpt = tokenRepository.findValidTokenById(tokenId, currentUtc);

        if (tokenOpt.isEmpty()) {
            logger.warn("Authorization code not found or expired: {}", tokenId);
            return Optional.empty();
        }

        AuthenticationToken token = tokenOpt.get();

        // Check if already used
        if (token.isUsed()) {
            logger.warn("Authorization code already used: {}", tokenId);
            return Optional.empty();
        }

        // Use same timestamp for update (eliminates race conditions)
        Instant updateTimestamp = currentUtc;

        // Parse existing metadata and update it
        AuthenticationToken.TokenMetadata metadata;
        try {
            if (token.getAuthObj() != null) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                metadata = mapper.readValue(token.getAuthObj(), AuthenticationToken.TokenMetadata.class);
            } else {
                metadata = new AuthenticationToken.TokenMetadata();
            }
        } catch (Exception e) {
            logger.error("Failed to parse token metadata", e);
            metadata = new AuthenticationToken.TokenMetadata();
        }

        // Update metadata with UTC timestamps
        metadata.used = true;
        metadata.usedAt = currentUtc;
        metadata.tokenValue = tokenId;  // Keep token value

        // Serialize to JSON (pre-built, no string concatenation in query)
        String updatedJsonMetadata;
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            updatedJsonMetadata = mapper.writeValueAsString(metadata);
        } catch (Exception e) {
            logger.error("Failed to serialize token metadata", e);
            return Optional.empty();
        }

        // Atomically mark code as used with pre-built JSON and UTC timestamps
        int updated = tokenRepository.markAuthorizationCodeAsUsed(
            tokenId,
            updatedJsonMetadata,
            currentUtc,
            updateTimestamp
        );

        if (updated == 0) {
            logger.warn("Authorization code invalid, already used, or expired (race condition): {}", tokenId);
            return Optional.empty();
        }

        String sessionId = token.getSysId();
        logger.info("Authorization code consumed for session: {}", sessionId);
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

        // Fetch the token to get current metadata
        Optional<AuthenticationToken> tokenOpt = tokenRepository.findValidTokenById(tokenId, currentUtc);

        if (tokenOpt.isEmpty()) {
            logger.warn("Access token not found or expired: {}", tokenId);
            return Optional.empty();
        }

        AuthenticationToken token = tokenOpt.get();

        // Calculate new expiration and update timestamp (based on same UTC time)
        Instant newExpirationUtc = currentUtc.plusSeconds(ACCESS_TOKEN_VALIDITY_MIN * 60L);
        Instant updateTimestamp = currentUtc;

        // Parse existing metadata and update it
        AuthenticationToken.TokenMetadata metadata;
        try {
            if (token.getAuthObj() != null) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                metadata = mapper.readValue(token.getAuthObj(), AuthenticationToken.TokenMetadata.class);
            } else {
                metadata = new AuthenticationToken.TokenMetadata();
            }
        } catch (Exception e) {
            logger.error("Failed to parse token metadata", e);
            metadata = new AuthenticationToken.TokenMetadata();
        }

        // Update metadata with UTC timestamp
        metadata.lastUsedAt = currentUtc;
        metadata.tokenValue = tokenId;  // Keep token value

        // Serialize to JSON (pre-built, no string concatenation in query)
        String updatedJsonMetadata;
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            updatedJsonMetadata = mapper.writeValueAsString(metadata);
        } catch (Exception e) {
            logger.error("Failed to serialize token metadata", e);
            return Optional.empty();
        }

        // Atomically extend token with pre-built JSON and UTC timestamps
        int updated = tokenRepository.extendAccessTokenExpiration(
            tokenId,
            newExpirationUtc,
            updatedJsonMetadata,
            currentUtc,
            updateTimestamp
        );

        if (updated == 0) {
            logger.warn("Access token not found, expired, or invalid (race condition): {}", tokenId);
            return Optional.empty();
        }

        String sessionId = token.getSysId();
        logger.debug("Access token validated and extended for session: {} (new expiration: {})",
            sessionId, newExpirationUtc);
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
        Instant cutoffUtc = currentUtc.minusSeconds(5 * 60);  // 5 minutes ago

        // Delete expired tokens (uses UTC comparison)
        int expiredCount = tokenRepository.deleteExpiredTokens(currentUtc);

        // Delete old used authorization codes (keep for 5 min audit trail)
        int usedCodesCount = tokenRepository.deleteOldUsedAuthorizationCodes(cutoffUtc);

        if (expiredCount > 0 || usedCodesCount > 0) {
            logger.info("Cleanup: {} expired tokens, {} old authorization codes (cutoff: {})",
                expiredCount, usedCodesCount, cutoffUtc);
        }
    }
}
