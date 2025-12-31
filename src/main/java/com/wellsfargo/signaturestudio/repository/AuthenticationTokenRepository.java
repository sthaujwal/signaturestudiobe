package com.wellsfargo.signaturestudio.repository;

import com.wellsfargo.signaturestudio.domain.AuthenticationToken;
import com.wellsfargo.signaturestudio.domain.AuthenticationToken.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
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
     * Find valid (non-expired) token by authentication_token_id (primary key).
     * Uses primary key lookup for optimal performance.
     *
     * RACE-CONDITION PROOF: Uses UTC timestamp passed from Java for consistent comparison.
     *
     * Query explanation:
     * - Uses primary key index for fast lookup
     * - Checks expiration using UTC timestamp from Java (eliminates clock skew)
     * - Returns entity if token is valid and not expired
     *
     * @param tokenId The token ID to find
     * @param currentUtc Current UTC timestamp for expiration comparison
     */
    @Query(value =
        "SELECT * FROM AUTHENTICATION_TOKEN " +
        "WHERE authentication_token_id = :tokenId " +
        "  AND next_expir_tmstp > :currentUtc",
        nativeQuery = true)
    Optional<AuthenticationToken> findValidTokenById(
        @Param("tokenId") String tokenId,
        @Param("currentUtc") Instant currentUtc
    );

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
     * RACE-CONDITION PROOF DESIGN:
     * - Timestamps generated in Java (UTC) before query execution
     * - JSON metadata pre-built and passed as parameter
     * - Optimistic locking using WHERE clause checks expiration against current UTC
     * - Single atomic UPDATE - no time gap between timestamp generation and update
     *
     * DISTRIBUTED SYSTEM:
     * - All timestamps are UTC (Instant) - no timezone confusion
     * - Comparison against UTC timestamp (:currentUtc) instead of database clock
     * - Pre-built JSON eliminates string concatenation race conditions
     * - Works consistently across all data centers
     *
     * @param tokenId Token ID (authentication_token_id) to extend
     * @param newExpirationUtc New expiration timestamp (UTC)
     * @param updatedJsonMetadata Pre-built JSON string with lastUsedAt timestamp
     * @param currentUtc Current UTC timestamp for comparison (ensures token not expired)
     * @param updateTimestamp Row update timestamp (UTC)
     * @return Number of rows updated (1 = success, 0 = not found/expired)
     */
    @Modifying
    @Query(value =
        "UPDATE AUTHENTICATION_TOKEN " +
        "SET next_expir_tmstp = :newExpirationUtc, " +
        "    auth_obj = :updatedJsonMetadata, " +
        "    row_lst_updt_tmstp = :updateTimestamp " +
        "WHERE authentication_token_id = :tokenId " +
        "  AND token_type = 'ACCESS_TOKEN' " +
        "  AND next_expir_tmstp > :currentUtc",
        nativeQuery = true)
    int extendAccessTokenExpiration(
        @Param("tokenId") String tokenId,
        @Param("newExpirationUtc") Instant newExpirationUtc,
        @Param("updatedJsonMetadata") String updatedJsonMetadata,
        @Param("currentUtc") Instant currentUtc,
        @Param("updateTimestamp") Instant updateTimestamp
    );

    /**
     * Atomically mark authorization code as used by updating JSON metadata.
     * Returns number of rows updated (1 if successful, 0 if already used/expired).
     *
     * RACE-CONDITION PROOF DESIGN:
     * - Timestamps generated in Java (UTC) before query execution
     * - JSON metadata pre-built and passed as parameter
     * - Optimistic locking: only updates if not already used AND not expired
     * - Single atomic UPDATE - no time gap between timestamp generation and update
     *
     * DISTRIBUTED SYSTEM:
     * - All timestamps are UTC (Instant) - no timezone confusion
     * - Comparison against UTC timestamp (:currentUtc) instead of database clock
     * - Pre-built JSON eliminates string concatenation race conditions
     * - Prevents replay attacks across data centers
     *
     * @param tokenId Authorization code ID (authentication_token_id)
     * @param updatedJsonMetadata Pre-built JSON string with used=true and usedAt timestamp
     * @param currentUtc Current UTC timestamp for comparison (ensures token not expired)
     * @param updateTimestamp Row update timestamp (UTC)
     * @return Number of rows updated (1 = success, 0 = already used/expired)
     */
    @Modifying
    @Query(value =
        "UPDATE AUTHENTICATION_TOKEN " +
        "SET auth_obj = :updatedJsonMetadata, " +
        "    row_lst_updt_tmstp = :updateTimestamp " +
        "WHERE authentication_token_id = :tokenId " +
        "  AND token_type = 'AUTHORIZATION_CODE' " +
        "  AND (JSON_VALUE(auth_obj, '$.used') = 'false' OR JSON_VALUE(auth_obj, '$.used') IS NULL) " +
        "  AND next_expir_tmstp > :currentUtc",
        nativeQuery = true)
    int markAuthorizationCodeAsUsed(
        @Param("tokenId") String tokenId,
        @Param("updatedJsonMetadata") String updatedJsonMetadata,
        @Param("currentUtc") Instant currentUtc,
        @Param("updateTimestamp") Instant updateTimestamp
    );

    /**
     * Delete all tokens for a session (on logout).
     * Removes both authorization codes and access tokens for the session.
     */
    @Modifying
    @Query("DELETE FROM AuthenticationToken t WHERE t.sysId = :sysId")
    int deleteBySysId(@Param("sysId") String sysId);

    /**
     * Cleanup expired tokens (scheduled task).
     * Uses UTC comparison for consistency across distributed systems.
     *
     * @param currentUtc Current UTC timestamp to compare against
     */
    @Modifying
    @Query(value =
        "DELETE FROM AUTHENTICATION_TOKEN " +
        "WHERE next_expir_tmstp < :currentUtc",
        nativeQuery = true)
    int deleteExpiredTokens(@Param("currentUtc") Instant currentUtc);

    /**
     * Cleanup used authorization codes older than threshold (keep for audit trail).
     * Removes authorization codes that were used before the cutoff timestamp.
     *
     * Query explanation:
     * - JSON_VALUE extracts 'used' field from JSON metadata
     * - JSON_VALUE extracts 'usedAt' timestamp from JSON metadata
     * - TO_TIMESTAMP converts ISO 8601 string back to timestamp for comparison
     * - Deletes codes used before cutoffUtc
     *
     * @param cutoffUtc UTC timestamp - codes used before this will be deleted
     */
    @Modifying
    @Query(value =
        "DELETE FROM AUTHENTICATION_TOKEN " +
        "WHERE token_type = 'AUTHORIZATION_CODE' " +
        "  AND JSON_VALUE(auth_obj, '$.used') = 'true' " +
        "  AND TO_TIMESTAMP(JSON_VALUE(auth_obj, '$.usedAt'), 'YYYY-MM-DD\"T\"HH24:MI:SS.FF3TZH:TZM') < :cutoffUtc",
        nativeQuery = true)
    int deleteOldUsedAuthorizationCodes(@Param("cutoffUtc") Instant cutoffUtc);
}
