package com.wellsfargo.signaturestudio.service;

import com.wellsfargo.signaturestudio.domain.AuthenticationToken;
import com.wellsfargo.signaturestudio.domain.AuthenticationToken.TokenType;
import com.wellsfargo.signaturestudio.repository.AuthenticationTokenRepository;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Authentication token service with hybrid approach:
 *
 * AUTHORIZATION CODES (Database):
 * - Short-lived (60 seconds)
 * - One-time use (deleted after consumption)
 * - Stored in AUTHENTICATION_TOKEN table
 * - Prevents replay attacks
 *
 * ACCESS TOKENS (Session Attributes):
 * - Lives as long as session (no expiration)
 * - Stored in SPRING_SESSION_ATTRIBUTES table
 * - Auto-cleaned when session expires
 * - No token refresh logic needed
 * - Faster validation (indexed session lookup)
 *
 * Flow:
 * 1. generateAuthorizationCode() - Create code in database
 * 2. validateAndConsumeAuthorizationCode() - Validate & delete code
 * 3. generateAccessTokenInSession() - Generate UUID, store in session attribute
 * 4. validateAccessToken() - Check if token matches session attribute
 * 5. Session expires - Spring auto-deletes session + attributes (token gone)
 */
@Service
public class AuthenticationTokenService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationTokenService.class);

    // Session attribute key for access token
    private static final String SESSION_ATTR_ACCESS_TOKEN = "ACCESS_TOKEN";
    private static final String SESSION_ATTR_TOKEN_CREATED_AT = "TOKEN_CREATED_AT";

    // Configuration constants
    private static final int AUTHORIZATION_CODE_VALIDITY_MIN = 1;  // 1 minute

    private final AuthenticationTokenRepository tokenRepository;

    public AuthenticationTokenService(AuthenticationTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    /**
     * Generate short-lived authorization code (60 seconds, one-time use).
     * Stored in database for security and one-time validation.
     *
     * @param sessionId The session ID to associate with the code
     * @return The generated authorization code value
     */
    public String generateAuthorizationCode(String sessionId) {
        // Generate UUID as token ID
        String tokenId = UUID.randomUUID().toString();

        // Create authorization code entity
        AuthenticationToken token = new AuthenticationToken();
        token.setAuthenticationTokenId(tokenId);
        token.setTokenType(TokenType.AUTHORIZATION_CODE);
        token.setSysId(sessionId);
        token.setAuthObj(sessionId);
        token.setExpirProdInMin(AUTHORIZATION_CODE_VALIDITY_MIN);
        token.setNextExpirTmstp(Instant.now().plusSeconds(AUTHORIZATION_CODE_VALIDITY_MIN * 60L));

        tokenRepository.save(token);

        logger.info("Generated authorization code for session: {} (expires in 1 minute)", sessionId);
        return tokenId;
    }

    /**
     * Validate and consume authorization code (ONE-TIME USE).
     * Returns session ID if valid, empty if invalid/expired.
     *
     * After successful validation:
     * - Authorization code is deleted (one-time use)
     * - Caller should generate access token and store in session
     *
     * @param codeId The authorization code ID to validate
     * @return Optional containing session ID if valid, empty otherwise
     */
    @Transactional
    public Optional<String> validateAndConsumeAuthorizationCode(String codeId) {
        Instant currentUtc = Instant.now();

        // Find valid authorization code
        Optional<AuthenticationToken> tokenOpt = tokenRepository
            .findByAuthenticationTokenIdAndTokenTypeAndNextExpirTmstpAfter(
                codeId,
                TokenType.AUTHORIZATION_CODE,
                currentUtc
            );

        if (tokenOpt.isEmpty()) {
            logger.warn("Authorization code not found or expired: {}", codeId);
            return Optional.empty();
        }

        AuthenticationToken token = tokenOpt.get();
        String sessionId = token.getSysId();

        // Delete the authorization code (one-time use)
        tokenRepository.delete(token);

        logger.info("Authorization code consumed and deleted for session: {}", sessionId);
        return Optional.of(sessionId);
    }

    /**
     * Generate access token and store in session attribute.
     *
     * NEW APPROACH:
     * - Token stored in SPRING_SESSION_ATTRIBUTES (not database)
     * - Token lives as long as session (no expiration)
     * - Automatically cleaned when session expires
     * - No token refresh needed
     * - Faster validation (session attribute lookup)
     *
     * @param session The HTTP session
     * @return The generated access token value
     */
    public String generateAccessTokenInSession(HttpSession session) {
        // Generate UUID as access token
        String accessToken = UUID.randomUUID().toString();

        // Store in session attributes
        session.setAttribute(SESSION_ATTR_ACCESS_TOKEN, accessToken);
        session.setAttribute(SESSION_ATTR_TOKEN_CREATED_AT, Instant.now());

        logger.info("Generated access token for session: {} (stored in session attribute)",
            session.getId());

        return accessToken;
    }

    /**
     * Validate access token against session attribute.
     *
     * NEW APPROACH:
     * - No database lookup required
     * - Just compare token from request with session attribute
     * - If session expired, attribute doesn't exist (returns false)
     * - If token doesn't match, returns false
     *
     * @param session The HTTP session (Spring Session auto-loaded)
     * @param tokenFromRequest The token from request header
     * @return true if token is valid, false otherwise
     */
    public boolean validateAccessToken(HttpSession session, String tokenFromRequest) {
        if (tokenFromRequest == null || tokenFromRequest.isBlank()) {
            logger.debug("Access token missing from request");
            return false;
        }

        if (session == null) {
            logger.debug("Session not found");
            return false;
        }

        // Get token from session attribute
        String sessionToken = (String) session.getAttribute(SESSION_ATTR_ACCESS_TOKEN);

        if (sessionToken == null) {
            logger.debug("No access token found in session: {}", session.getId());
            return false;
        }

        // Compare tokens
        boolean valid = tokenFromRequest.equals(sessionToken);

        if (valid) {
            logger.debug("Access token validated for session: {}", session.getId());
        } else {
            logger.warn("Access token mismatch for session: {}", session.getId());
        }

        return valid;
    }

    /**
     * Revoke access token from session.
     * Called on explicit logout.
     *
     * Note: If session is invalidated, Spring automatically clears all attributes,
     * so this is optional (belt and suspenders approach).
     *
     * @param session The HTTP session
     */
    public void revokeAccessToken(HttpSession session) {
        if (session != null) {
            session.removeAttribute(SESSION_ATTR_ACCESS_TOKEN);
            session.removeAttribute(SESSION_ATTR_TOKEN_CREATED_AT);
            logger.info("Revoked access token from session: {}", session.getId());
        }
    }

    /**
     * Cleanup expired authorization codes only.
     * Runs every 5 minutes.
     *
     * Note: Access tokens no longer need cleanup (stored in session attributes,
     * auto-deleted by Spring Session when session expires).
     */
    @Scheduled(fixedRate = 300000)  // Every 5 minutes
    @Transactional
    public void cleanupExpiredAuthorizationCodes() {
        Instant currentUtc = Instant.now();

        // Delete only expired authorization codes (access tokens no longer in this table)
        Long expiredCount = tokenRepository.deleteByNextExpirTmstpBefore(currentUtc);

        if (expiredCount != null && expiredCount > 0) {
            logger.info("Cleanup: {} expired authorization code(s) deleted", expiredCount);
        }
    }
}
