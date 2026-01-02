# Oracle + Java Instant + Hibernate Guide

## The Problem: Oracle and Java Instant

### Why Oracle Struggles with Instant

**Java `Instant`:**
- Represents a specific moment in UTC (absolute time)
- Always timezone-aware (implicitly UTC)
- Example: `2026-01-02T10:30:45Z`

**Oracle `TIMESTAMP`:**
- Timezone-naive (just date + time, no timezone info)
- Example: `2026-01-02 10:30:45`

**The Mismatch:**
When using native SQL queries, Oracle's JDBC driver doesn't know how to automatically convert `Instant` → `TIMESTAMP`, causing errors like:
```
ORA-18716: {0} not in any timezone.DATE
```

## The Solution: Our Configuration

### 1. Use Oracle12cDialect (Not Generic OracleDialect)

**application.properties:**
```properties
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.Oracle12cDialect
spring.jpa.properties.hibernate.jdbc.time_zone=UTC
```

**What this does:**
- `Oracle12cDialect`: Uses modern Oracle features and proper JDBC 4.2 temporal type handling
- `hibernate.jdbc.time_zone=UTC`: Ensures all timestamps are stored/retrieved in UTC timezone

### 2. Use Spring Data JPA Method Names (Not Native Queries)

**❌ BAD - Native Query (Causes ORA-18716):**
```java
@Query(value = "SELECT * FROM AUTHENTICATION_TOKEN " +
               "WHERE authentication_token_id = :tokenId " +
               "AND next_expir_tmstp > :currentUtc",
       nativeQuery = true)
Optional<AuthenticationToken> findValidToken(
    @Param("tokenId") String tokenId,
    @Param("currentUtc") Instant currentUtc  // ❌ Oracle JDBC can't convert this!
);
```

**✅ GOOD - Spring Data JPA Method Name:**
```java
Optional<AuthenticationToken> findByAuthenticationTokenIdAndNextExpirTmstpAfter(
    String authenticationTokenId,
    Instant currentUtc  // ✅ Hibernate converts automatically!
);
```

**Why this works:**
- Hibernate's `Oracle12cDialect` provides automatic conversion: `Instant` ↔ `java.sql.Timestamp` ↔ Oracle `TIMESTAMP`
- Spring Data JPA generates JPQL (not native SQL), which goes through Hibernate's type system
- No manual `CAST(:param AS TIMESTAMP)` needed!

### 3. Database Schema: Use TIMESTAMP (Not TIMESTAMP WITH TIME ZONE)

**✅ CORRECT:**
```sql
CREATE TABLE AUTHENTICATION_TOKEN (
    -- Use plain TIMESTAMP (Hibernate handles UTC conversion)
    next_expir_tmstp TIMESTAMP NOT NULL,
    row_crte_tmstp TIMESTAMP NOT NULL,
    row_lst_updt_tmstp TIMESTAMP NOT NULL
);
```

**❌ WRONG:**
```sql
CREATE TABLE AUTHENTICATION_TOKEN (
    -- Don't use TIMESTAMP WITH TIME ZONE (causes more complexity)
    next_expir_tmstp TIMESTAMP WITH TIME ZONE NOT NULL
);
```

**Why plain TIMESTAMP?**
- Hibernate's `Instant` mapping expects plain `TIMESTAMP`
- The `hibernate.jdbc.time_zone=UTC` property ensures UTC is used consistently
- Simpler and more portable across databases

### 4. Java Entity: Use java.time.Instant

**✅ CORRECT:**
```java
@Entity
@Table(name = "AUTHENTICATION_TOKEN")
public class AuthenticationToken {

    @Column(name = "next_expir_tmstp", nullable = false)
    private Instant nextExpirTmstp;  // ✅ Use Instant everywhere

    @Column(name = "row_crte_tmstp", nullable = false, updatable = false)
    private Instant rowCrteTmstp;

    @Column(name = "row_lst_updt_tmstp", nullable = false)
    private Instant rowLstUpdtTmstp;
}
```

**❌ WRONG:**
```java
// Don't use LocalDateTime (timezone-ambiguous!)
private LocalDateTime nextExpirTmstp;  // ❌ What timezone is this?

// Don't use java.sql.Timestamp (legacy API)
private Timestamp nextExpirTmstp;  // ❌ Use java.time API instead
```

## How It Works: The Conversion Chain

### Saving to Database

```
Java Code:                Hibernate:              Oracle:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
token.setNextExpirTmstp(  →  Convert to         →  Store as
  Instant.now()              java.sql.Timestamp     TIMESTAMP
)                            using UTC timezone     (value represents
                                                     UTC moment)
```

**Example:**
```java
// Your code:
Instant now = Instant.now();  // 2026-01-02T10:30:45Z (UTC)
token.setNextExpirTmstp(now);
tokenRepository.save(token);

// Hibernate converts:
// Instant → java.sql.Timestamp (with UTC offset)
// → Oracle stores: TIMESTAMP '2026-01-02 10:30:45'
```

### Querying from Database

```
Java Code:                Spring/Hibernate:              Oracle:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Instant currentUtc =      →  Generate JPQL with      →  Execute:
  Instant.now();             parameter binding          SELECT *
                             Convert Instant to         WHERE
repository.                  java.sql.Timestamp         next_expir_tmstp > ?
  findBy...After(             using UTC timezone        (? = '2026-01-02 10:30:45')
    currentUtc
  )                       ←  Convert result back     ←  Returns matching rows
                             TIMESTAMP → Instant
                             using UTC timezone
```

**Example:**
```java
// Your code:
Instant currentUtc = Instant.now();  // 2026-01-02T10:30:45Z
Optional<AuthenticationToken> tokenOpt = tokenRepository
    .findByAuthenticationTokenIdAndNextExpirTmstpAfter(tokenId, currentUtc);

// Hibernate generates JPQL:
// SELECT t FROM AuthenticationToken t
// WHERE t.authenticationTokenId = ?1 AND t.nextExpirTmstp > ?2

// Oracle sees:
// SELECT * FROM AUTHENTICATION_TOKEN
// WHERE authentication_token_id = ? AND next_expir_tmstp > ?
// (Hibernate provides java.sql.Timestamp parameter)
```

## Consistency Guarantee

### How UTC Consistency is Maintained

**Key Principle:** Always use `Instant.now()` everywhere in Java code.

| Operation | Java Code | What Gets Stored/Compared |
|-----------|-----------|---------------------------|
| Token creation | `Instant.now().plusSeconds(30*60)` | UTC moment + 30min |
| Token extension | `Instant.now().plusSeconds(30*60)` | UTC moment + 30min |
| Validation query | `Instant.now()` | Current UTC moment |
| Cleanup query | `Instant.now()` | Current UTC moment |
| @PrePersist | `Instant.now()` | Current UTC moment |
| @PreUpdate | `Instant.now()` | Current UTC moment |

**Result:** All timestamps are in UTC, so comparisons are always correct!

### Multi-Datacenter Consistency

Even if servers run in different timezones:

```java
// Server in New York (EST, UTC-5) - Local time: 05:30 AM EST
Instant.now()  // Returns: 2026-01-02T10:30:00Z (UTC)

// Server in London (GMT, UTC+0) - Local time: 10:30 AM GMT
Instant.now()  // Returns: 2026-01-02T10:30:00Z (UTC)

// Server in Tokyo (JST, UTC+9) - Local time: 07:30 PM JST
Instant.now()  // Returns: 2026-01-02T10:30:00Z (UTC)
```

**All servers produce the same `Instant` at the same moment!**

## What NOT to Do

### ❌ DON'T: Use Native Queries with Instant Parameters

```java
// ❌ This will cause ORA-18716 error!
@Query(value = "SELECT * FROM AUTHENTICATION_TOKEN " +
               "WHERE next_expir_tmstp > :currentUtc",
       nativeQuery = true)
List<AuthenticationToken> findExpired(@Param("currentUtc") Instant currentUtc);
```

### ❌ DON'T: Mix LocalDateTime and Instant

```java
// ❌ LocalDateTime is timezone-ambiguous!
LocalDateTime now = LocalDateTime.now();  // What timezone?
token.setNextExpirTmstp(now);  // Type error - can't assign LocalDateTime to Instant
```

### ❌ DON'T: Use Oracle SYSTIMESTAMP in Queries

```java
// ❌ Database system time might not be UTC!
@Query(value = "UPDATE AUTHENTICATION_TOKEN " +
               "SET next_expir_tmstp = SYSTIMESTAMP + INTERVAL '30' MINUTE " +
               "WHERE authentication_token_id = :tokenId",
       nativeQuery = true)
int extend(@Param("tokenId") String tokenId);
```

### ❌ DON'T: Use java.sql.Timestamp Directly

```java
// ❌ Legacy API - use java.time.Instant instead
import java.sql.Timestamp;

Timestamp now = new Timestamp(System.currentTimeMillis());  // Don't do this!
token.setNextExpirTmstp(now);  // Type error
```

## Testing the Configuration

### Verify Instant Conversion Works

```java
@SpringBootTest
class AuthenticationTokenRepositoryTest {

    @Autowired
    private AuthenticationTokenRepository tokenRepository;

    @Test
    void testInstantConversion() {
        // Create token with Instant timestamp
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(1800);  // 30 minutes

        AuthenticationToken token = new AuthenticationToken();
        token.setAuthenticationTokenId(UUID.randomUUID().toString());
        token.setTokenType(TokenType.ACCESS_TOKEN);
        token.setSysId("test-session");
        token.setAuthObj("test-session");
        token.setExpirProdInMin(30);
        token.setNextExpirTmstp(expiration);

        // Save to Oracle
        tokenRepository.save(token);

        // Query using Instant (this would fail with ORA-18716 if config is wrong)
        Optional<AuthenticationToken> result = tokenRepository
            .findByAuthenticationTokenIdAndNextExpirTmstpAfter(
                token.getAuthenticationTokenId(),
                now  // Instant parameter
            );

        // Should find the token
        assertTrue(result.isPresent());
        assertEquals(expiration, result.get().getNextExpirTmstp());
    }
}
```

## Summary

**Our Configuration Fixes the Oracle + Instant Issue:**

1. ✅ **Oracle12cDialect** - Proper Instant/TIMESTAMP mapping
2. ✅ **hibernate.jdbc.time_zone=UTC** - Consistent UTC timezone
3. ✅ **Spring Data JPA Method Names** - No native SQL with Instant params
4. ✅ **Plain TIMESTAMP columns** - Hibernate handles UTC conversion
5. ✅ **Always use Instant.now()** - UTC timestamps everywhere

**Result:**
- No ORA-18716 errors
- No manual CAST needed
- Consistent UTC timestamps in database
- Works across multiple datacenters
- Database-agnostic (can switch to PostgreSQL/MySQL easily)

---

**Document Version:** 1.0
**Last Updated:** 2026-01-02
**Status:** Production-Ready
