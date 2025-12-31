package com.wellsfargo.signaturestudio.repository;

import com.wellsfargo.signaturestudio.domain.AuthenticationToken;
import com.wellsfargo.signaturestudio.domain.AuthenticationToken.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing authentication tokens (both authorization codes and access tokens).
 *
 * DISTRIBUTED SYSTEM SUPPORT:
 * - Uses atomic database operations to prevent race conditions
 * - Uses database timestamps (SYSTIMESTAMP) to eliminate clock skew
 * - All update operations are single-query for consistency
 */
@Repository
public interface AuthenticationTokenRepository extends JpaRepository<AuthenticationToken, String> {

    /**
     * Find token by auth_obj (the actual token value).
     * Used for validation when token is provided in requests.
     *
     * NOTE: Use findValidToken() for validation to check expiration atomically.
     */
    Optional<AuthenticationToken> findByAuthObj(String authObj);

    /**
     * Find valid (non-expired) token by auth_obj.
     * Uses database timestamp to check expiration (eliminates clock skew issues).
     *
     * DISTRIBUTED SYSTEM: Safe across multiple data centers - uses Oracle's clock.
     */
    @Query(value =
        "SELECT * FROM AUTHENTICATION_TOKENS " +
        "WHERE auth_obj = :authObj " +
        "  AND next_expir_tmstp > SYSTIMESTAMP",
        nativeQuery = true)
    Optional<AuthenticationToken> findValidToken(@Param("authObj") String authObj);

    /**
     * Find all tokens for a session (used for revocation).
     */
    List<AuthenticationToken> findAllBySysId(String sysId);

    /**
     * Find tokens by session ID and type.
     * Useful for checking if a session already has tokens of a specific type.
     */
    Optional<AuthenticationToken> findBySysIdAndTokenType(String sysId, TokenType tokenType);

    /**
     * Atomically extend access token expiration using database timestamp.
     * Returns number of rows updated (1 if successful, 0 if token not found/expired).
     *
     * DISTRIBUTED SYSTEM:
     * - Atomic operation - no race conditions across data centers
     * - Uses Oracle SYSTIMESTAMP - eliminates clock skew
     * - Single database round-trip - better performance
     * - Conditional update - only extends if token is still valid
     *
     * @param authObj Token value to extend
     * @param validityMinutes How many minutes to extend (e.g., 30)
     * @return Number of rows updated (1 = success, 0 = not found/expired)
     */
    @Modifying
    @Query(value =
        "UPDATE AUTHENTICATION_TOKENS " +
        "SET next_expir_tmstp = SYSTIMESTAMP + INTERVAL ':validityMinutes' MINUTE, " +
        "    last_used_tmstp = SYSTIMESTAMP, " +
        "    row_lst_updt_tmstp = SYSTIMESTAMP " +
        "WHERE auth_obj = :authObj " +
        "  AND token_type = 'ACCESS_TOKEN' " +
        "  AND next_expir_tmstp > SYSTIMESTAMP",
        nativeQuery = true)
    int extendAccessTokenExpiration(
        @Param("authObj") String authObj,
        @Param("validityMinutes") int validityMinutes
    );

    /**
     * Atomically mark authorization code as used.
     * Returns number of rows updated (1 if successful, 0 if already used/expired).
     *
     * DISTRIBUTED SYSTEM:
     * - Atomic operation - prevents replay attacks across data centers
     * - Uses database timestamp for consistency
     * - Conditional update - only marks if not already used
     *
     * @param authObj Authorization code value
     * @return Number of rows updated (1 = success, 0 = already used/expired)
     */
    @Modifying
    @Query(value =
        "UPDATE AUTHENTICATION_TOKENS " +
        "SET used_at = SYSTIMESTAMP, " +
        "    row_lst_updt_tmstp = SYSTIMESTAMP " +
        "WHERE auth_obj = :authObj " +
        "  AND token_type = 'AUTHORIZATION_CODE' " +
        "  AND used_at IS NULL " +
        "  AND next_expir_tmstp > SYSTIMESTAMP",
        nativeQuery = true)
    int markAuthorizationCodeAsUsed(@Param("authObj") String authObj);

    /**
     * Delete all tokens for a session (on logout).
     * Removes both authorization codes and access tokens for the session.
     */
    @Modifying
    @Query("DELETE FROM AuthenticationToken t WHERE t.sysId = :sysId")
    int deleteBySysId(@Param("sysId") String sysId);

    /**
     * Cleanup expired tokens (scheduled task).
     * Uses database timestamp to check expiration (eliminates clock skew).
     *
     * DISTRIBUTED SYSTEM: Safe - uses Oracle's clock for consistency.
     */
    @Modifying
    @Query(value =
        "DELETE FROM AUTHENTICATION_TOKENS " +
        "WHERE next_expir_tmstp < SYSTIMESTAMP",
        nativeQuery = true)
    int deleteExpiredTokens();

    /**
     * Cleanup used authorization codes older than threshold (keep for audit trail).
     * Removes authorization codes that were used more than X minutes ago.
     *
     * DISTRIBUTED SYSTEM: Safe - uses Oracle's clock for consistency.
     *
     * @param retentionMinutes How long to keep used codes (e.g., 5 minutes)
     */
    @Modifying
    @Query(value =
        "DELETE FROM AUTHENTICATION_TOKENS " +
        "WHERE token_type = 'AUTHORIZATION_CODE' " +
        "  AND used_at IS NOT NULL " +
        "  AND used_at < SYSTIMESTAMP - INTERVAL ':retentionMinutes' MINUTE",
        nativeQuery = true)
    int deleteOldUsedAuthorizationCodes(@Param("retentionMinutes") int retentionMinutes);
}
