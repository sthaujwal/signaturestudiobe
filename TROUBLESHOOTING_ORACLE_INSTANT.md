# Troubleshooting: Oracle + Instant Issues

## Problem

Getting errors when querying with `Instant` parameters in Oracle database, such as:
- ORA-18716: {0} not in any timezone.DATE
- ORA-01861: literal does not match format string
- SQL Error attempting to convert Instant to TIMESTAMP

## Solutions (Try in Order)

### Solution 1: Custom AttributeConverter (RECOMMENDED) ✅

We've already added this to your project:

**File:** `src/main/java/com/wellsfargo/signaturestudio/config/InstantAttributeConverter.java`

```java
@Converter(autoApply = true)
public class InstantAttributeConverter implements AttributeConverter<Instant, Timestamp> {
    @Override
    public Timestamp convertToDatabaseColumn(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    @Override
    public Instant convertToEntityAttribute(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
```

**What it does:**
- Explicitly tells Hibernate how to convert `Instant` ↔ `Timestamp`
- Auto-applies to all `Instant` fields via `@Converter(autoApply = true)`
- Works with Spring Data JPA method names

**After adding:** Restart your application and test again.

---

### Solution 2: Verify Application Properties

**File:** `src/main/resources/application.properties`

Ensure these settings are present:

```properties
# Use Oracle12cDialect (not generic OracleDialect!)
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.Oracle12cDialect

# Set timezone to UTC for consistent Instant handling
spring.jpa.properties.hibernate.jdbc.time_zone=UTC

# Optional: Enable SQL logging to see generated queries
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

---

### Solution 3: Update build.gradle Dependencies

**File:** `build.gradle`

Ensure you have the correct Oracle JDBC driver version:

```gradle
dependencies {
    // Use latest ojdbc11 with full JDBC 4.2 support
    runtimeOnly 'com.oracle.database.jdbc:ojdbc11:23.9.0.24.10'

    // Make sure Spring Boot Data JPA is included
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
}
```

**After changing:** Run `./gradlew clean build --refresh-dependencies`

---

### Solution 4: Verify Database Column Type

**Check your Oracle table definition:**

```sql
-- Show table structure
DESC AUTHENTICATION_TOKEN;

-- Check column type
SELECT column_name, data_type, data_length
FROM user_tab_columns
WHERE table_name = 'AUTHENTICATION_TOKEN'
AND column_name IN ('NEXT_EXPIR_TMSTP', 'ROW_CRTE_TMSTP', 'ROW_LST_UPDT_TMSTP');
```

**Expected output:**
```
COLUMN_NAME           DATA_TYPE    DATA_LENGTH
--------------------- ------------ -----------
NEXT_EXPIR_TMSTP      TIMESTAMP    11
ROW_CRTE_TMSTP        TIMESTAMP    11
ROW_LST_UPDT_TMSTP    TIMESTAMP    11
```

**If column type is wrong:**
```sql
-- Fix column type
ALTER TABLE AUTHENTICATION_TOKEN MODIFY next_expir_tmstp TIMESTAMP;
ALTER TABLE AUTHENTICATION_TOKEN MODIFY row_crte_tmstp TIMESTAMP;
ALTER TABLE AUTHENTICATION_TOKEN MODIFY row_lst_updt_tmstp TIMESTAMP;
```

---

### Solution 5: Check for Conflicting Query Methods

**Look for any remaining native queries in your repository:**

```bash
# Search for native queries
grep -r "@Query.*nativeQuery.*true" src/main/java/
```

**If found, replace with Spring Data JPA method names:**

```java
// ❌ BAD - Native query with Instant
@Query(value = "SELECT * FROM ... WHERE next_expir_tmstp > :instant", nativeQuery = true)
List<Token> findExpired(@Param("instant") Instant instant);

// ✅ GOOD - Spring Data JPA method name
List<Token> findByNextExpirTmstpAfter(Instant instant);
```

---

### Solution 6: Enable Debug Logging

**Add to application.properties:**

```properties
# Enable Hibernate SQL logging
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# Enable Spring Data JPA logging
logging.level.org.springframework.data.jpa=DEBUG
```

**This will show:**
- Generated SQL queries
- Parameter bindings
- Type conversions

**Example output:**
```
Hibernate: select ... from authentication_token where ... and next_expir_tmstp>?
TRACE - binding parameter [2] as [TIMESTAMP] - [2026-01-02 10:30:45.123]
```

**Look for errors in parameter binding.**

---

### Solution 7: Annotate Entity Fields Explicitly

**If AttributeConverter doesn't work, try explicit @Temporal:**

```java
@Entity
@Table(name = "AUTHENTICATION_TOKEN")
public class AuthenticationToken {

    @Column(name = "next_expir_tmstp", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)  // Add this
    private Instant nextExpirTmstp;
}
```

**Note:** With modern Hibernate (6.x), this shouldn't be necessary, but worth trying.

---

### Solution 8: Use OffsetDateTime Instead (Alternative)

**If Instant continues to fail, switch to OffsetDateTime:**

```java
@Entity
@Table(name = "AUTHENTICATION_TOKEN")
public class AuthenticationToken {

    @Column(name = "next_expir_tmstp", nullable = false)
    private OffsetDateTime nextExpirTmstp;  // Instead of Instant

    // When setting:
    public void extendExpiration(int validityMinutes) {
        this.nextExpirTmstp = OffsetDateTime.now(ZoneOffset.UTC)
            .plusMinutes(validityMinutes);
    }
}
```

**Repository:**
```java
Optional<AuthenticationToken> findByAuthenticationTokenIdAndNextExpirTmstpAfter(
    String authenticationTokenId,
    OffsetDateTime currentUtc  // Instead of Instant
);
```

**Service:**
```java
@Transactional
public Optional<String> validateAndExtendAccessToken(String tokenId) {
    OffsetDateTime currentUtc = OffsetDateTime.now(ZoneOffset.UTC);  // Instead of Instant.now()

    Optional<AuthenticationToken> tokenOpt = tokenRepository
        .findByAuthenticationTokenIdAndTokenTypeAndNextExpirTmstpAfter(
            tokenId, TokenType.ACCESS_TOKEN, currentUtc
        );
    // ...
}
```

**Pros:**
- `OffsetDateTime` has explicit timezone info
- Often better supported by Oracle JDBC drivers
- Still represents UTC (via `ZoneOffset.UTC`)

**Cons:**
- Need to change entity, repository, and service code
- Slightly more verbose than `Instant`

---

### Solution 9: Manual JPQL Query (Last Resort)

**If Spring method names don't work, use JPQL:**

```java
@Repository
public interface AuthenticationTokenRepository extends JpaRepository<AuthenticationToken, String> {

    @Query("SELECT t FROM AuthenticationToken t " +
           "WHERE t.authenticationTokenId = :tokenId " +
           "AND t.tokenType = :tokenType " +
           "AND t.nextExpirTmstp > :currentUtc")
    Optional<AuthenticationToken> findValidToken(
        @Param("tokenId") String tokenId,
        @Param("tokenType") TokenType tokenType,
        @Param("currentUtc") Instant currentUtc
    );
}
```

**Note:** This is JPQL (not native SQL), so Hibernate handles type conversion.

---

## Debugging Steps

### Step 1: Test Basic Save/Load

```java
@SpringBootTest
class InstantConversionTest {

    @Autowired
    private AuthenticationTokenRepository tokenRepository;

    @Test
    void testBasicSaveAndLoad() {
        // Create token
        AuthenticationToken token = new AuthenticationToken();
        token.setAuthenticationTokenId(UUID.randomUUID().toString());
        token.setTokenType(TokenType.ACCESS_TOKEN);
        token.setSysId("test-session");
        token.setAuthObj("test-session");
        token.setExpirProdInMin(30);
        token.setNextExpirTmstp(Instant.now().plusSeconds(1800));

        // Save
        tokenRepository.save(token);

        // Load by ID (doesn't use Instant parameter)
        Optional<AuthenticationToken> loaded = tokenRepository.findById(token.getAuthenticationTokenId());

        assertTrue(loaded.isPresent());
        assertNotNull(loaded.get().getNextExpirTmstp());

        System.out.println("✅ Basic save/load works!");
    }
}
```

**If this fails:** Problem is in entity/Hibernate configuration.

### Step 2: Test Query with Instant Parameter

```java
@Test
void testQueryWithInstantParameter() {
    // Create token
    Instant now = Instant.now();
    Instant expiration = now.plusSeconds(1800);

    AuthenticationToken token = new AuthenticationToken();
    token.setAuthenticationTokenId(UUID.randomUUID().toString());
    token.setTokenType(TokenType.ACCESS_TOKEN);
    token.setSysId("test-session");
    token.setAuthObj("test-session");
    token.setExpirProdInMin(30);
    token.setNextExpirTmstp(expiration);

    tokenRepository.save(token);

    // Query using Instant parameter (THIS IS WHERE IT MIGHT FAIL)
    Optional<AuthenticationToken> result = tokenRepository
        .findByAuthenticationTokenIdAndNextExpirTmstpAfter(
            token.getAuthenticationTokenId(),
            now  // Instant parameter
        );

    assertTrue(result.isPresent());
    System.out.println("✅ Query with Instant parameter works!");
}
```

**If this fails:** Problem is in repository query method or Hibernate parameter binding.

### Step 3: Check Generated SQL

**Enable SQL logging and look at the console output:**

```
Hibernate:
    select
        a1_0.authentication_token_id,
        a1_0.auth_obj,
        a1_0.expir_prod_in_min,
        a1_0.next_expir_tmstp,
        ...
    from
        authentication_token a1_0
    where
        a1_0.authentication_token_id=?
        and a1_0.next_expir_tmstp>?

TRACE - binding parameter [1] as [VARCHAR] - [abc-123-def]
TRACE - binding parameter [2] as [TIMESTAMP] - [2026-01-02 10:30:45.123]
```

**Look for:**
- ✅ Parameter [2] should be bound as `[TIMESTAMP]`
- ❌ If it shows `[VARCHAR]` or `[DATE]`, there's a type mapping issue

---

## What to Report

If none of the solutions work, please provide:

1. **Full error stack trace:**
```
java.sql.SQLException: ORA-18716: ...
    at oracle.jdbc...
    at org.hibernate...
    ...
```

2. **Hibernate SQL log output:**
```
Hibernate: select ... where next_expir_tmstp>?
TRACE - binding parameter [X] as [TYPE] - [VALUE]
```

3. **Oracle database version:**
```sql
SELECT * FROM v$version;
```

4. **Hibernate version:**
```bash
./gradlew dependencies | grep hibernate
```

5. **JDBC driver version:**
```bash
./gradlew dependencies | grep ojdbc
```

---

## Expected Configuration (Summary)

**build.gradle:**
```gradle
runtimeOnly 'com.oracle.database.jdbc:ojdbc11:23.9.0.24.10'
```

**application.properties:**
```properties
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.Oracle12cDialect
spring.jpa.properties.hibernate.jdbc.time_zone=UTC
```

**InstantAttributeConverter.java:**
```java
@Converter(autoApply = true)
public class InstantAttributeConverter implements AttributeConverter<Instant, Timestamp> { ... }
```

**Oracle table:**
```sql
CREATE TABLE AUTHENTICATION_TOKEN (
    next_expir_tmstp TIMESTAMP NOT NULL,
    ...
);
```

**Entity:**
```java
@Column(name = "next_expir_tmstp", nullable = false)
private Instant nextExpirTmstp;
```

**Repository:**
```java
Optional<AuthenticationToken> findByAuthenticationTokenIdAndNextExpirTmstpAfter(
    String authenticationTokenId,
    Instant currentUtc
);
```

**Service:**
```java
Instant currentUtc = Instant.now();
repository.findBy...After(..., currentUtc);
```

---

**Document Version:** 1.0
**Last Updated:** 2026-01-02
