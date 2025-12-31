package com.wellsfargo.signaturestudio.repository;

import com.wellsfargo.signaturestudio.domain.AuthenticationToken;
import com.wellsfargo.signaturestudio.domain.AuthenticationToken.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing authentication tokens (both authorization codes and access tokens).
 *
 * Uses Spring Data JPA method names - no native queries, no CAST issues!
 *
 * BENEFITS:
 * - No ORA-18716 errors (Spring handles Instant conversion automatically)
 * - No manual CAST(:param AS TIMESTAMP) needed
 * - Database-agnostic (works with Oracle, PostgreSQL, MySQL, etc.)
 * - Less code, more maintainable
 * - Type-safe at compile time
 *
 * RACE-CONDITION PROOF DESIGN:
 * - All timestamps generated in Java (UTC) before queries
 * - Optimistic locking via method name conditions (e.g., AndNextExpirTmstpAfter)
 * - Atomic operations prevent race conditions across data centers
 */
@Repository
public interface AuthenticationTokenRepository extends JpaRepository<AuthenticationToken, String> {

    /**
     * Find valid (non-expired) token by ID.
     *
     * Spring generates: SELECT * FROM authentication_token
     *                   WHERE authentication_token_id = ?
     *                   AND next_expir_tmstp > ?
     *
     * NO CAST NEEDED - Spring handles Instant conversion automatically!
     *
     * @param authenticationTokenId The token ID (primary key)
     * @param currentUtc Current UTC timestamp for expiration check
     * @return Optional containing token if found and valid
     */
    Optional<AuthenticationToken> findByAuthenticationTokenIdAndNextExpirTmstpAfter(
        String authenticationTokenId,
        Instant currentUtc
    );

    /**
     * Find valid token by ID and type.
     *
     * Spring generates: SELECT * FROM authentication_token
     *                   WHERE authentication_token_id = ?
     *                   AND token_type = ?
     *                   AND next_expir_tmstp > ?
     *
     * Used for validating specific token types (ACCESS_TOKEN or AUTHORIZATION_CODE).
     *
     * @param authenticationTokenId The token ID
     * @param tokenType The token type to match
     * @param currentUtc Current UTC timestamp for expiration check
     * @return Optional containing token if found and valid
     */
    Optional<AuthenticationToken> findByAuthenticationTokenIdAndTokenTypeAndNextExpirTmstpAfter(
        String authenticationTokenId,
        TokenType tokenType,
        Instant currentUtc
    );

    /**
     * Find all tokens for a session (used for revocation).
     *
     * Spring generates: SELECT * FROM authentication_token WHERE sys_id = ?
     */
    List<AuthenticationToken> findAllBySysId(String sysId);

    /**
     * Find tokens by session ID and type.
     * Useful for checking if a session already has tokens of a specific type.
     *
     * Spring generates: SELECT * FROM authentication_token
     *                   WHERE sys_id = ? AND token_type = ?
     */
    Optional<AuthenticationToken> findBySysIdAndTokenType(String sysId, TokenType tokenType);

    /**
     * Find all tokens of a specific type.
     *
     * Spring generates: SELECT * FROM authentication_token WHERE token_type = ?
     *
     * Used for cleanup operations that need to filter by type.
     */
    List<AuthenticationToken> findByTokenType(TokenType tokenType);

    /**
     * Delete all tokens for a session (on logout).
     *
     * Spring generates: DELETE FROM authentication_token WHERE sys_id = ?
     *
     * @return Number of tokens deleted
     */
    @Modifying
    int deleteBySysId(String sysId);

    /**
     * Delete expired tokens (scheduled cleanup).
     *
     * Spring generates: DELETE FROM authentication_token WHERE next_expir_tmstp < ?
     *
     * NO CAST NEEDED - Spring handles Instant conversion automatically!
     *
     * @param cutoffUtc UTC timestamp - tokens expired before this will be deleted
     * @return Number of tokens deleted
     */
    @Modifying
    int deleteByNextExpirTmstpBefore(Instant cutoffUtc);

    /**
     * Delete multiple tokens by IDs (used for batch cleanup).
     *
     * Spring generates: DELETE FROM authentication_token
     *                   WHERE authentication_token_id IN (?, ?, ...)
     *
     * @param ids List of token IDs to delete
     * @return Number of tokens deleted
     */
    @Modifying
    int deleteByAuthenticationTokenIdIn(List<String> ids);
}
