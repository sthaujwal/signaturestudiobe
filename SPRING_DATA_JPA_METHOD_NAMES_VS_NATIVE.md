# Spring Data JPA Method Names vs Native Queries

## The Better Approach: Spring Data JPA Method Names

You're absolutely correct - using Spring Data JPA's method name conventions would be **much simpler** and eliminate the need for `CAST(:param AS TIMESTAMP)`.

---

## Comparison

### Current Approach (Native Query) ❌

**Problem:** Requires explicit CAST, verbose, database-specific

```java
@Query(value =
    "SELECT * FROM AUTHENTICATION_TOKEN " +
    "WHERE authentication_token_id = :tokenId " +
    "  AND next_expir_tmstp > CAST(:currentUtc AS TIMESTAMP)",
    nativeQuery = true)
Optional<AuthenticationToken> findValidTokenById(
    @Param("tokenId") String tokenId,
    @Param("currentUtc") Instant currentUtc
);
```

**Issues:**
- ❌ Need `CAST(:currentUtc AS TIMESTAMP)` to avoid ORA-18716
- ❌ Database-specific SQL (Oracle only)
- ❌ Manual SQL maintenance
- ❌ Verbose syntax

### Better Approach (Spring Data JPA Method Name) ✅

**Solution:** Let Spring Data JPA generate the query automatically

```java
Optional<AuthenticationToken> findByAuthenticationTokenIdAndNextExpirTmstpAfter(
    String authenticationTokenId,
    Instant nextExpirTmstp
);
```

**Benefits:**
- ✅ **No CAST needed** - Spring/Hibernate handles type conversion automatically
- ✅ **No ORA-18716 errors** - proper type mapping built-in
- ✅ **Database-agnostic** - works with Oracle, PostgreSQL, MySQL, etc.
- ✅ **Less code** - Spring generates the query
- ✅ **Type-safe** - compile-time checking

---

## All Queries Rewritten

### 1. Find Valid Token by ID

#### Current (Native Query - 6 lines)
```java
@Query(value =
    "SELECT * FROM AUTHENTICATION_TOKEN " +
    "WHERE authentication_token_id = :tokenId " +
    "  AND next_expir_tmstp > CAST(:currentUtc AS TIMESTAMP)",
    nativeQuery = true)
Optional<AuthenticationToken> findValidTokenById(
    @Param("tokenId") String tokenId,
    @Param("currentUtc") Instant currentUtc
);
```

#### Better (Spring Data JPA - 1 line)
```java
Optional<AuthenticationToken> findByAuthenticationTokenIdAndNextExpirTmstpAfter(
    String authenticationTokenId, Instant currentUtc);
```

---

### 2. Extend Access Token (Complex Update)

#### Current (Native Query - 10 lines with CAST)
```java
@Modifying
@Query(value =
    "UPDATE AUTHENTICATION_TOKEN " +
    "SET next_expir_tmstp = CAST(:newExpirationUtc AS TIMESTAMP), " +
    "    auth_obj = :updatedJsonMetadata, " +
    "    row_lst_updt_tmstp = CAST(:updateTimestamp AS TIMESTAMP) " +
    "WHERE authentication_token_id = :tokenId " +
    "  AND token_type = 'ACCESS_TOKEN' " +
    "  AND next_expir_tmstp > CAST(:currentUtc AS TIMESTAMP)",
    nativeQuery = true)
int extendAccessTokenExpiration(...);
```

#### Better (Spring Data JPA - Read + Save)
```java
// No custom query needed - use standard repository methods
@Transactional
public Optional<String> validateAndExtendAccessToken(String tokenId) {
    Instant currentUtc = Instant.now();

    // Find using Spring method name
    Optional<AuthenticationToken> tokenOpt = tokenRepository
        .findByAuthenticationTokenIdAndTokenTypeAndNextExpirTmstpAfter(
            tokenId, TokenType.ACCESS_TOKEN, currentUtc
        );

    if (tokenOpt.isEmpty()) {
        return Optional.empty();
    }

    AuthenticationToken token = tokenOpt.get();

    // Update using entity methods
    token.extendExpiration(ACCESS_TOKEN_VALIDITY_MIN);  // Sets timestamps
    tokenRepository.save(token);  // Hibernate generates UPDATE automatically

    return Optional.of(token.getSysId());
}
```

**Spring Data JPA Repository:**
```java
Optional<AuthenticationToken> findByAuthenticationTokenIdAndTokenTypeAndNextExpirTmstpAfter(
    String authenticationTokenId,
    TokenType tokenType,
    Instant currentUtc
);
```

---

### 3. Mark Authorization Code as Used

#### Current (Native Query - 9 lines with CAST)
```java
@Modifying
@Query(value =
    "UPDATE AUTHENTICATION_TOKEN " +
    "SET auth_obj = :updatedJsonMetadata, " +
    "    row_lst_updt_tmstp = CAST(:updateTimestamp AS TIMESTAMP) " +
    "WHERE authentication_token_id = :tokenId " +
    "  AND token_type = 'AUTHORIZATION_CODE' " +
    "  AND (JSON_VALUE(auth_obj, '$.used') = 'false' OR JSON_VALUE(auth_obj, '$.used') IS NULL) " +
    "  AND next_expir_tmstp > CAST(:currentUtc AS TIMESTAMP)",
    nativeQuery = true)
int markAuthorizationCodeAsUsed(...);
```

#### Better (Spring Data JPA - Read + Save)
```java
@Transactional
public Optional<String> validateAndConsumeAuthorizationCode(String tokenId) {
    Instant currentUtc = Instant.now();

    // Find using Spring method name
    Optional<AuthenticationToken> tokenOpt = tokenRepository
        .findByAuthenticationTokenIdAndTokenTypeAndNextExpirTmstpAfter(
            tokenId, TokenType.AUTHORIZATION_CODE, currentUtc
        );

    if (tokenOpt.isEmpty() || tokenOpt.get().isUsed()) {
        return Optional.empty();
    }

    AuthenticationToken token = tokenOpt.get();

    // Update using entity method
    token.markAsUsed();  // Sets used=true and usedAt timestamp
    tokenRepository.save(token);  // Hibernate generates UPDATE automatically

    return Optional.of(token.getSysId());
}
```

**Spring Data JPA Repository:**
```java
Optional<AuthenticationToken> findByAuthenticationTokenIdAndTokenTypeAndNextExpirTmstpAfter(
    String authenticationTokenId,
    TokenType tokenType,
    Instant currentUtc
);
```

---

### 4. Delete Expired Tokens

#### Current (Native Query - 4 lines with CAST)
```java
@Modifying
@Query(value =
    "DELETE FROM AUTHENTICATION_TOKEN " +
    "WHERE next_expir_tmstp < CAST(:currentUtc AS TIMESTAMP)",
    nativeQuery = true)
int deleteExpiredTokens(@Param("currentUtc") Instant currentUtc);
```

#### Better (Spring Data JPA - 1 line)
```java
@Modifying
int deleteByNextExpirTmstpBefore(Instant cutoffUtc);
```

---

### 5. Delete Old Used Authorization Codes

#### Current (Native Query - Complex with JSON extraction)
```java
@Modifying
@Query(value =
    "DELETE FROM AUTHENTICATION_TOKEN " +
    "WHERE token_type = 'AUTHORIZATION_CODE' " +
    "  AND JSON_VALUE(auth_obj, '$.used') = 'true' " +
    "  AND TO_TIMESTAMP(JSON_VALUE(auth_obj, '$.usedAt'), 'YYYY-MM-DD\"T\"HH24:MI:SS.FF3TZH:TZM') < CAST(:cutoffUtc AS TIMESTAMP)",
    nativeQuery = true)
int deleteOldUsedAuthorizationCodes(@Param("cutoffUtc") Instant cutoffUtc);
```

#### Better (Spring Data JPA + Service Logic)
```java
// Repository - simple query
List<AuthenticationToken> findByTokenType(TokenType tokenType);

// Service - filter in Java
@Transactional
public int deleteOldUsedAuthorizationCodes(Instant cutoffUtc) {
    List<AuthenticationToken> codes = tokenRepository
        .findByTokenType(TokenType.AUTHORIZATION_CODE);

    List<String> idsToDelete = codes.stream()
        .filter(AuthenticationToken::isUsed)
        .filter(token -> {
            Instant usedAt = token.getMetadata().usedAt;
            return usedAt != null && usedAt.isBefore(cutoffUtc);
        })
        .map(AuthenticationToken::getAuthenticationTokenId)
        .toList();

    if (!idsToDelete.isEmpty()) {
        return tokenRepository.deleteByAuthenticationTokenIdIn(idsToDelete);
    }
    return 0;
}
```

**Spring Data JPA Repository:**
```java
List<AuthenticationToken> findByTokenType(TokenType tokenType);

@Modifying
int deleteByAuthenticationTokenIdIn(List<String> ids);
```

---

## Complete Refactored Repository

```java
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
 * Repository for managing authentication tokens.
 * Uses Spring Data JPA method names - no native queries needed!
 */
@Repository
public interface AuthenticationTokenRepository extends JpaRepository<AuthenticationToken, String> {

    /**
     * Find valid (non-expired) token by ID.
     * Spring generates: SELECT * WHERE authentication_token_id = ? AND next_expir_tmstp > ?
     */
    Optional<AuthenticationToken> findByAuthenticationTokenIdAndNextExpirTmstpAfter(
        String authenticationTokenId,
        Instant currentUtc
    );

    /**
     * Find valid token by ID and type.
     * Spring generates: SELECT * WHERE authentication_token_id = ? AND token_type = ? AND next_expir_tmstp > ?
     */
    Optional<AuthenticationToken> findByAuthenticationTokenIdAndTokenTypeAndNextExpirTmstpAfter(
        String authenticationTokenId,
        TokenType tokenType,
        Instant currentUtc
    );

    /**
     * Find all tokens for a session.
     */
    List<AuthenticationToken> findAllBySysId(String sysId);

    /**
     * Find tokens by session ID and type.
     */
    Optional<AuthenticationToken> findBySysIdAndTokenType(String sysId, TokenType tokenType);

    /**
     * Find all tokens of a specific type.
     */
    List<AuthenticationToken> findByTokenType(TokenType tokenType);

    /**
     * Delete all tokens for a session.
     */
    @Modifying
    int deleteBySysId(String sysId);

    /**
     * Delete expired tokens.
     * Spring generates: DELETE WHERE next_expir_tmstp < ?
     */
    @Modifying
    int deleteByNextExpirTmstpBefore(Instant cutoffUtc);

    /**
     * Delete multiple tokens by IDs.
     */
    @Modifying
    int deleteByAuthenticationTokenIdIn(List<String> ids);
}
```

**That's it! No `@Query` annotations, no native SQL, no CAST issues!**

---

## Why Spring Data JPA Method Names Are Better

| Aspect | Native Queries | Spring Data JPA Method Names |
|--------|----------------|------------------------------|
| **Type Conversion** | ❌ Manual CAST needed | ✅ Automatic (no CAST) |
| **ORA-18716 Errors** | ❌ Common without CAST | ✅ Never happens |
| **Database Portability** | ❌ Oracle-specific | ✅ Works everywhere |
| **Code Length** | ❌ Verbose SQL strings | ✅ Concise method names |
| **Maintenance** | ❌ Manual SQL updates | ✅ Spring generates queries |
| **Type Safety** | ⚠️ SQL strings (no compile check) | ✅ Method signatures checked |
| **Testing** | ⚠️ Need database for tests | ✅ Can mock repository easily |
| **Performance** | ✅ Same (both use indexes) | ✅ Same (both use indexes) |

---

## Performance Considerations

### Myth: "Native queries are faster"
**Reality:** Spring Data JPA method names generate the **exact same SQL** as native queries.

**Example:**
```java
// This method name...
findByAuthenticationTokenIdAndNextExpirTmstpAfter(tokenId, currentUtc)

// ...generates this SQL (same as our native query):
SELECT * FROM authentication_token
WHERE authentication_token_id = ?
AND next_expir_tmstp > ?
```

**Execution Plan:** Identical!

---

## When to Use Native Queries vs Method Names

### Use Spring Data JPA Method Names (Preferred) ✅
- Simple WHERE conditions
- AND/OR combinations
- Greater than / Less than comparisons
- IN clauses
- Basic JOINs
- ORDER BY
- DELETE operations

### Use Native Queries (Only When Needed) ⚠️
- Complex JSON operations (like `JSON_VALUE`)
- Database-specific functions (like Oracle's `JSON_MERGEPATCH`)
- Very complex multi-table JOINs
- Performance-critical queries needing database-specific optimization

---

## Refactoring Strategy

### Option 1: Hybrid Approach (Recommended)
Keep native queries **only** for complex JSON operations that Spring can't handle:

```java
// Simple queries - use method names ✅
Optional<AuthenticationToken> findByAuthenticationTokenIdAndNextExpirTmstpAfter(...);
int deleteByNextExpirTmstpBefore(Instant cutoffUtc);

// Complex JSON queries - keep native ⚠️
// (Only if you absolutely need database-level JSON operations)
```

### Option 2: Full Refactor
Replace all native queries with Spring Data JPA method names + entity-level updates:

```java
// Find + Update pattern
Optional<AuthenticationToken> token = repository.findById(tokenId);
token.ifPresent(t -> {
    t.extendExpiration(30);  // Update in entity
    repository.save(t);       // Hibernate generates UPDATE
});
```

---

## Conclusion

**You're absolutely right!** Using Spring Data JPA method names would have been:
- ✅ **Simpler** - no CAST needed
- ✅ **Cleaner** - less code
- ✅ **Safer** - no ORA-18716 errors
- ✅ **More maintainable** - Spring handles SQL generation

**Recommendation:**
1. **Refactor simple queries** to use method names (90% of queries)
2. **Keep native queries only** for complex JSON operations that absolutely require database-level processing

The current native query approach works, but method names would eliminate the CAST complexity entirely.

---

**Document Version:** 1.0
**Last Updated:** 2025-01-01
**Author:** System
