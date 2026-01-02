# Oracle Instant Parameter Fix

## Problem: ORA-18716 Error

When using `java.time.Instant` parameters in native Oracle queries, you may encounter:

```
ORA-18716: {0} not in any timezone.DATE
```

This happens because Spring Data JPA doesn't automatically convert `Instant` to Oracle `TIMESTAMP` in native queries.

---

## Solution: Explicit CAST

**Always wrap `Instant` parameters with `CAST(:param AS TIMESTAMP)`**

### Example Queries

#### ❌ Wrong (throws ORA-18716):
```java
@Query(value =
    "SELECT * FROM AUTHENTICATION_TOKEN " +
    "WHERE next_expir_tmstp > :currentUtc",
    nativeQuery = true)
Optional<AuthenticationToken> findValidTokenById(
    @Param("currentUtc") Instant currentUtc
);
```

#### ✅ Correct (works):
```java
@Query(value =
    "SELECT * FROM AUTHENTICATION_TOKEN " +
    "WHERE next_expir_tmstp > CAST(:currentUtc AS TIMESTAMP)",
    nativeQuery = true)
Optional<AuthenticationToken> findValidTokenById(
    @Param("currentUtc") Instant currentUtc
);
```

---

## All Fixed Queries

### 1. findValidTokenById
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

### 2. extendAccessTokenExpiration
```java
@Query(value =
    "UPDATE AUTHENTICATION_TOKEN " +
    "SET next_expir_tmstp = CAST(:newExpirationUtc AS TIMESTAMP), " +
    "    auth_obj = :updatedJsonMetadata, " +
    "    row_lst_updt_tmstp = CAST(:updateTimestamp AS TIMESTAMP) " +
    "WHERE authentication_token_id = :tokenId " +
    "  AND token_type = 'ACCESS_TOKEN' " +
    "  AND next_expir_tmstp > CAST(:currentUtc AS TIMESTAMP)",
    nativeQuery = true)
int extendAccessTokenExpiration(
    @Param("tokenId") String tokenId,
    @Param("newExpirationUtc") Instant newExpirationUtc,
    @Param("updatedJsonMetadata") String updatedJsonMetadata,
    @Param("currentUtc") Instant currentUtc,
    @Param("updateTimestamp") Instant updateTimestamp
);
```

### 3. markAuthorizationCodeAsUsed
```java
@Query(value =
    "UPDATE AUTHENTICATION_TOKEN " +
    "SET auth_obj = :updatedJsonMetadata, " +
    "    row_lst_updt_tmstp = CAST(:updateTimestamp AS TIMESTAMP) " +
    "WHERE authentication_token_id = :tokenId " +
    "  AND token_type = 'AUTHORIZATION_CODE' " +
    "  AND (JSON_VALUE(auth_obj, '$.used') = 'false' OR JSON_VALUE(auth_obj, '$.used') IS NULL) " +
    "  AND next_expir_tmstp > CAST(:currentUtc AS TIMESTAMP)",
    nativeQuery = true)
int markAuthorizationCodeAsUsed(
    @Param("tokenId") String tokenId,
    @Param("updatedJsonMetadata") String updatedJsonMetadata,
    @Param("currentUtc") Instant currentUtc,
    @Param("updateTimestamp") Instant updateTimestamp
);
```

### 4. deleteExpiredTokens
```java
@Query(value =
    "DELETE FROM AUTHENTICATION_TOKEN " +
    "WHERE next_expir_tmstp < CAST(:currentUtc AS TIMESTAMP)",
    nativeQuery = true)
int deleteExpiredTokens(@Param("currentUtc") Instant currentUtc);
```

### 5. deleteOldUsedAuthorizationCodes
```java
@Query(value =
    "DELETE FROM AUTHENTICATION_TOKEN " +
    "WHERE token_type = 'AUTHORIZATION_CODE' " +
    "  AND JSON_VALUE(auth_obj, '$.used') = 'true' " +
    "  AND TO_TIMESTAMP(JSON_VALUE(auth_obj, '$.usedAt'), 'YYYY-MM-DD\"T\"HH24:MI:SS.FF3TZH:TZM') < CAST(:cutoffUtc AS TIMESTAMP)",
    nativeQuery = true)
int deleteOldUsedAuthorizationCodes(@Param("cutoffUtc") Instant cutoffUtc);
```

---

## Why This Happens

### Spring Data JPA Entity Mapping (✅ Works)
When JPA loads/saves entities, Hibernate automatically converts:
```java
@Column(name = "next_expir_tmstp")
private Instant nextExpirTmstp;  // ← Hibernate handles conversion
```

### Native Query Parameters (❌ Broken Without CAST)
In native SQL, Spring passes `Instant` as-is to Oracle JDBC driver:
```sql
-- Java: Instant.now() = 2025-01-01T14:30:00Z
-- JDBC: Passes as java.time.Instant object
-- Oracle: Cannot convert Instant → TIMESTAMP automatically
-- Result: ORA-18716
```

### With CAST (✅ Works)
```sql
CAST(:currentUtc AS TIMESTAMP)
-- JDBC: Passes Instant as string or timestamp
-- Oracle: CAST explicitly converts to TIMESTAMP
-- Result: Success
```

---

## Configuration Check

Ensure your `application.yml` has:

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          time_zone: UTC  # Forces UTC timezone for all JDBC operations
```

This ensures consistent UTC handling across:
- Entity field mapping
- Native query parameters
- Database storage

---

## Testing

### Unit Test
```java
@Test
void testInstantParameterMapping() {
    Instant now = Instant.now();
    String tokenId = "test-uuid-123";
    
    // Should not throw ORA-18716
    Optional<AuthenticationToken> token = 
        tokenRepository.findValidTokenById(tokenId, now);
    
    // Should work correctly
    assertNotNull(token);
}
```

### SQL Verification
```sql
-- Test that CAST works correctly
SELECT 
    authentication_token_id,
    next_expir_tmstp,
    CASE 
        WHEN next_expir_tmstp > CAST(SYSTIMESTAMP AS TIMESTAMP) 
        THEN 'VALID' 
        ELSE 'EXPIRED' 
    END AS status
FROM AUTHENTICATION_TOKEN;
```

---

## Summary

| Scenario | Query Syntax | Result |
|----------|--------------|--------|
| **Entity Field** | `@Column private Instant field;` | ✅ Works (Hibernate converts) |
| **Native Query WITHOUT CAST** | `WHERE timestamp > :instant` | ❌ ORA-18716 |
| **Native Query WITH CAST** | `WHERE timestamp > CAST(:instant AS TIMESTAMP)` | ✅ Works |
| **JPQL Query** | `WHERE t.timestamp > :instant` | ✅ Works (Hibernate converts) |

**Rule:** Always use `CAST(:param AS TIMESTAMP)` for `Instant` in native Oracle queries.

---

**Document Version:** 1.0  
**Last Updated:** 2025-01-01  
**Author:** System
