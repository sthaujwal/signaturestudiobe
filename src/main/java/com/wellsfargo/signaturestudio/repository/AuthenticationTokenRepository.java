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
 * - Works with JSON metadata stored in auth_obj CLOB
 */
@Repository
public interface AuthenticationTokenRepository extends JpaRepository<AuthenticationToken, String> {

    /**
     * Find valid (non-expired) token where JSON contains the specified token value.
     * Uses JSON_VALUE to extract tokenValue from the CLOB and check expiration atomically.
     *
     * DISTRIBUTED SYSTEM: Safe across multiple data centers - uses Oracle's clock.
     *
     * Query explanation:
     * - JSON_VALUE(auth_obj, '$.tokenValue') extracts tokenValue from JSON in CLOB
     * - Checks expiration using SYSTIMESTAMP (database clock)
     * - Returns entity if token is valid and not expired
     */
    @Query(value =
        "SELECT * FROM AUTHENTICATION_TOKENS " +
        "WHERE JSON_VALUE(auth_obj, '$.tokenValue') = :tokenValue " +
        "  AND next_expir_tmstp > SYSTIMESTAMP",
        nativeQuery = true)
    Optional<AuthenticationToken> findValidTokenByValue(@Param("tokenValue") String tokenValue);

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
     * Atomically extend access token expiration and update lastUsedAt in JSON metadata.
     * Returns number of rows updated (1 if successful, 0 if token not found/expired).
     *
     * DISTRIBUTED SYSTEM:
     * - Atomic operation - no race conditions across data centers
     * - Uses Oracle SYSTIMESTAMP - eliminates clock skew
     * - Single database round-trip - better performance
     * - Conditional update - only extends if token is still valid
     * - Updates JSON metadata to record last usage time
     *
     * Query explanation:
     * - JSON_VALUE extracts tokenValue from CLOB for matching
     * - JSON_TRANSFORM updates 'lastUsedAt' field in JSON without replacing entire CLOB
     * - SYSTIMESTAMP ensures consistent timing across all data centers
     *
     * @param tokenValue Token value to extend
     * @param validityMinutes How many minutes to extend (e.g., 30)
     * @return Number of rows updated (1 = success, 0 = not found/expired)
     */
    @Modifying
    @Query(value =
        "UPDATE AUTHENTICATION_TOKENS " +
        "SET next_expir_tmstp = SYSTIMESTAMP + NUMTODSINTERVAL(:validityMinutes, 'MINUTE'), " +
        "    auth_obj = JSON_TRANSFORM(auth_obj, SET '$.lastUsedAt' = TO_CHAR(SYSTIMESTAMP, 'YYYY-MM-DD\"T\"HH24:MI:SS.FF3\"Z\"')), " +
        "    row_lst_updt_tmstp = SYSTIMESTAMP " +
        "WHERE JSON_VALUE(auth_obj, '$.tokenValue') = :tokenValue " +
        "  AND token_type = 'ACCESS_TOKEN' " +
        "  AND next_expir_tmstp > SYSTIMESTAMP",
        nativeQuery = true)
    int extendAccessTokenExpiration(
        @Param("tokenValue") String tokenValue,
        @Param("validityMinutes") int validityMinutes
    );

    /**
     * Atomically mark authorization code as used by updating JSON metadata.
     * Returns number of rows updated (1 if successful, 0 if already used/expired).
     *
     * DISTRIBUTED SYSTEM:
     * - Atomic operation - prevents replay attacks across data centers
     * - Uses database timestamp for consistency
     * - Conditional update - only marks if not already used
     * - Updates JSON metadata to set 'used' = true and 'usedAt' timestamp
     *
     * Query explanation:
     * - JSON_VALUE extracts tokenValue for matching
     * - JSON_VALUE checks if 'used' is false (authorization code not yet consumed)
     * - JSON_TRANSFORM updates both 'used' and 'usedAt' fields in single operation
     * - SYSTIMESTAMP ensures consistent timing across all data centers
     *
     * @param tokenValue Authorization code value
     * @return Number of rows updated (1 = success, 0 = already used/expired)
     */
    @Modifying
    @Query(value =
        "UPDATE AUTHENTICATION_TOKENS " +
        "SET auth_obj = JSON_TRANSFORM(auth_obj, " +
        "       SET '$.used' = 'true', " +
        "       SET '$.usedAt' = TO_CHAR(SYSTIMESTAMP, 'YYYY-MM-DD\"T\"HH24:MI:SS.FF3\"Z\"')), " +
        "    row_lst_updt_tmstp = SYSTIMESTAMP " +
        "WHERE JSON_VALUE(auth_obj, '$.tokenValue') = :tokenValue " +
        "  AND token_type = 'AUTHORIZATION_CODE' " +
        "  AND (JSON_VALUE(auth_obj, '$.used') = 'false' OR JSON_VALUE(auth_obj, '$.used') IS NULL) " +
        "  AND next_expir_tmstp > SYSTIMESTAMP",
        nativeQuery = true)
    int markAuthorizationCodeAsUsed(@Param("tokenValue") String tokenValue);

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
     * Query explanation:
     * - JSON_VALUE extracts 'used' field from JSON metadata
     * - JSON_VALUE extracts 'usedAt' timestamp from JSON metadata
     * - TO_TIMESTAMP converts ISO 8601 string back to timestamp for comparison
     * - Deletes codes used more than retentionMinutes ago
     *
     * @param retentionMinutes How long to keep used codes (e.g., 5 minutes)
     */
    @Modifying
    @Query(value =
        "DELETE FROM AUTHENTICATION_TOKENS " +
        "WHERE token_type = 'AUTHORIZATION_CODE' " +
        "  AND JSON_VALUE(auth_obj, '$.used') = 'true' " +
        "  AND TO_TIMESTAMP(JSON_VALUE(auth_obj, '$.usedAt'), 'YYYY-MM-DD\"T\"HH24:MI:SS.FF3\"Z\"') < SYSTIMESTAMP - NUMTODSINTERVAL(:retentionMinutes, 'MINUTE')",
        nativeQuery = true)
    int deleteOldUsedAuthorizationCodes(@Param("retentionMinutes") int retentionMinutes);
}
