# CLOB JSON Implementation Guide

## Overview

The authentication token system stores JSON metadata in the `auth_obj` CLOB field instead of using separate database columns. This approach provides flexibility and works with Oracle's JSON functions for atomic operations.

---

## Database Schema

### Actual AUTHENTICATION_TOKENS Table

```sql
CREATE TABLE AUTHENTICATION_TOKENS (
    authentication_token_id VARCHAR2(64) PRIMARY KEY,
    token_type VARCHAR2(20) NOT NULL,  -- 'AUTHORIZATION_CODE' or 'ACCESS_TOKEN'
    auth_obj CLOB NOT NULL,  -- JSON metadata (see structure below)
    sys_id VARCHAR2(255) NOT NULL,  -- Session ID (FK to SPRING_SESSION)
    expir_prod_in_min NUMBER(10) NOT NULL,  -- Validity in minutes
    next_expir_tmstp TIMESTAMP NOT NULL,  -- Expiration timestamp
    row_crte_tmstp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    row_lst_updt_tmstp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT chk_token_type CHECK (token_type IN ('AUTHORIZATION_CODE', 'ACCESS_TOKEN')),
    CONSTRAINT fk_auth_token_session FOREIGN KEY (sys_id)
        REFERENCES SPRING_SESSION(SESSION_ID) ON DELETE CASCADE
);
```

### JSON Structure in auth_obj CLOB

```json
{
  "tokenValue": "abc123xyz...",
  "used": false,
  "usedAt": null,
  "lastUsedAt": null
}
```

**Field Descriptions:**

| Field | Type | Purpose | Token Type |
|-------|------|---------|------------|
| `tokenValue` | String | The actual token value (Base64-encoded random bytes) | Both |
| `used` | Boolean | Whether authorization code has been consumed | AUTHORIZATION_CODE |
| `usedAt` | ISO 8601 String | Timestamp when code was used | AUTHORIZATION_CODE |
| `lastUsedAt` | ISO 8601 String | Last API request timestamp | ACCESS_TOKEN |

---

## JPA Entity Implementation

### AuthenticationToken.java

```java
@Entity
@Table(name = "AUTHENTICATION_TOKENS")
public class AuthenticationToken {

    @Lob
    @Column(name = "auth_obj", nullable = false)
    private String authObj;  // CLOB field containing JSON

    @Transient
    private TokenMetadata metadata;  // Parsed JSON (not stored in DB)

    @PostLoad
    protected void parseMetadata() {
        // Parse JSON from CLOB after loading from database
        this.metadata = objectMapper.readValue(authObj, TokenMetadata.class);
    }

    public String getTokenValue() {
        ensureMetadataLoaded();
        return metadata != null ? metadata.tokenValue : null;
    }

    public void setTokenValue(String tokenValue) {
        ensureMetadataLoaded();
        metadata.tokenValue = tokenValue;
        updateAuthObjFromMetadata();  // Serialize back to JSON
    }

    /**
     * Inner class for JSON metadata structure.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TokenMetadata {
        public String tokenValue;
        public boolean used = false;
        public Instant usedAt;
        public Instant lastUsedAt;
    }
}
```

---

## Oracle JSON Functions Used

### 1. JSON_VALUE - Extract Field from JSON

**Purpose:** Read a specific field from the JSON CLOB.

```sql
-- Extract token value from JSON
JSON_VALUE(auth_obj, '$.tokenValue')

-- Extract 'used' flag from JSON
JSON_VALUE(auth_obj, '$.used')
```

**Example Query:**
```sql
SELECT * FROM AUTHENTICATION_TOKENS
WHERE JSON_VALUE(auth_obj, '$.tokenValue') = 'abc123'
  AND next_expir_tmstp > SYSTIMESTAMP;
```

### 2. JSON_TRANSFORM - Update JSON Field Atomically

**Purpose:** Update specific fields in JSON without replacing entire CLOB.

```sql
-- Mark authorization code as used
JSON_TRANSFORM(auth_obj,
    SET '$.used' = 'true',
    SET '$.usedAt' = TO_CHAR(SYSTIMESTAMP, 'YYYY-MM-DD"T"HH24:MI:SS.FF3"Z"')
)

-- Update lastUsedAt for access token
JSON_TRANSFORM(auth_obj,
    SET '$.lastUsedAt' = TO_CHAR(SYSTIMESTAMP, 'YYYY-MM-DD"T"HH24:MI:SS.FF3"Z"')
)
```

**Example UPDATE:**
```sql
UPDATE AUTHENTICATION_TOKENS
SET auth_obj = JSON_TRANSFORM(auth_obj,
        SET '$.used' = 'true',
        SET '$.usedAt' = TO_CHAR(SYSTIMESTAMP, 'YYYY-MM-DD"T"HH24:MI:SS.FF3"Z"')),
    row_lst_updt_tmstp = SYSTIMESTAMP
WHERE JSON_VALUE(auth_obj, '$.tokenValue') = :tokenValue
  AND token_type = 'AUTHORIZATION_CODE'
  AND (JSON_VALUE(auth_obj, '$.used') = 'false' OR JSON_VALUE(auth_obj, '$.used') IS NULL)
  AND next_expir_tmstp > SYSTIMESTAMP;
```

### 3. TO_TIMESTAMP - Parse ISO 8601 String

**Purpose:** Convert JSON timestamp string back to TIMESTAMP for comparisons.

```sql
-- Convert usedAt string to TIMESTAMP for comparison
TO_TIMESTAMP(JSON_VALUE(auth_obj, '$.usedAt'), 'YYYY-MM-DD"T"HH24:MI:SS.FF3"Z"')
```

**Example DELETE:**
```sql
DELETE FROM AUTHENTICATION_TOKENS
WHERE token_type = 'AUTHORIZATION_CODE'
  AND JSON_VALUE(auth_obj, '$.used') = 'true'
  AND TO_TIMESTAMP(JSON_VALUE(auth_obj, '$.usedAt'), 'YYYY-MM-DD"T"HH24:MI:SS.FF3"Z"')
      < SYSTIMESTAMP - NUMTODSINTERVAL(5, 'MINUTE');
```

---

## Repository Queries with JSON

### Find Token by Value

```java
@Query(value =
    "SELECT * FROM AUTHENTICATION_TOKENS " +
    "WHERE JSON_VALUE(auth_obj, '$.tokenValue') = :tokenValue " +
    "  AND next_expir_tmstp > SYSTIMESTAMP",
    nativeQuery = true)
Optional<AuthenticationToken> findValidTokenByValue(@Param("tokenValue") String tokenValue);
```

### Atomically Mark Authorization Code as Used

```java
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
```

### Atomically Extend Access Token

```java
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
```

### Cleanup Old Used Authorization Codes

```java
@Modifying
@Query(value =
    "DELETE FROM AUTHENTICATION_TOKENS " +
    "WHERE token_type = 'AUTHORIZATION_CODE' " +
    "  AND JSON_VALUE(auth_obj, '$.used') = 'true' " +
    "  AND TO_TIMESTAMP(JSON_VALUE(auth_obj, '$.usedAt'), 'YYYY-MM-DD\"T\"HH24:MI:SS.FF3\"Z\"') < SYSTIMESTAMP - NUMTODSINTERVAL(:retentionMinutes, 'MINUTE')",
    nativeQuery = true)
int deleteOldUsedAuthorizationCodes(@Param("retentionMinutes") int retentionMinutes);
```

---

## Service Layer Usage

### Generating Tokens

```java
private String generateToken(String sessionId, TokenType tokenType, int lengthBytes, int validityMinutes) {
    // Generate random token value
    byte[] randomBytes = new byte[lengthBytes];
    secureRandom.nextBytes(randomBytes);
    String tokenValue = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

    // Create entity
    AuthenticationToken token = new AuthenticationToken();
    token.setAuthenticationTokenId(UUID.randomUUID().toString());
    token.setTokenType(tokenType);
    token.setSysId(sessionId);
    token.setExpirProdInMin(validityMinutes);
    token.setNextExpirTmstp(Instant.now().plusSeconds(validityMinutes * 60L));

    // Set token value (will be serialized to JSON in auth_obj CLOB)
    token.setTokenValue(tokenValue);

    tokenRepository.save(token);
    return tokenValue;
}
```

**Resulting JSON in Database:**
```json
{
  "tokenValue": "eJxVkF2PgjAUhu_5FU3vm6K4q...",
  "used": false,
  "usedAt": null,
  "lastUsedAt": null
}
```

### Consuming Authorization Code

```java
@Transactional
public Optional<String> validateAndConsumeAuthorizationCode(String codeValue) {
    // Atomically mark as used using JSON_TRANSFORM
    int updated = tokenRepository.markAuthorizationCodeAsUsed(codeValue);

    if (updated == 0) {
        logger.warn("Authorization code invalid, already used, or expired");
        return Optional.empty();
    }

    // Fetch session ID
    Optional<AuthenticationToken> tokenOpt = tokenRepository.findValidTokenByValue(codeValue);

    if (tokenOpt.isPresent()) {
        String sessionId = tokenOpt.get().getSysId();
        logger.info("Authorization code consumed for session: {}", sessionId);
        return Optional.of(sessionId);
    }

    return Optional.empty();
}
```

**Database Changes:**
- BEFORE: `{"tokenValue": "abc123", "used": false, "usedAt": null}`
- AFTER: `{"tokenValue": "abc123", "used": true, "usedAt": "2025-01-01T10:05:00.123Z"}`

### Extending Access Token

```java
@Transactional
public Optional<String> validateAndExtendAccessToken(String tokenValue) {
    // Atomically extend expiration and update lastUsedAt using JSON_TRANSFORM
    int updated = tokenRepository.extendAccessTokenExpiration(
        tokenValue,
        ACCESS_TOKEN_VALIDITY_MIN
    );

    if (updated == 0) {
        logger.warn("Access token not found, expired, or invalid");
        return Optional.empty();
    }

    // Fetch session ID
    Optional<AuthenticationToken> tokenOpt = tokenRepository.findValidTokenByValue(tokenValue);

    if (tokenOpt.isPresent()) {
        String sessionId = tokenOpt.get().getSysId();
        logger.debug("Access token validated and extended for session: {}", sessionId);
        return Optional.of(sessionId);
    }

    return Optional.empty();
}
```

**Database Changes:**
- `next_expir_tmstp` updated to SYSTIMESTAMP + 30 minutes
- JSON updated: `{"tokenValue": "xyz789", "lastUsedAt": "2025-01-01T10:15:00.456Z"}`

---

## Distributed System Benefits

### 1. Atomic Operations with JSON_TRANSFORM

✅ **Race Condition Prevention**

```
Timeline:
T+0ms   DC1: Mark code as used → JSON_TRANSFORM updates {"used": true}
T+1ms   DC2: Mark code as used → WHERE clause fails (used = true) → Returns 0
```

**Why it works:**
- `JSON_TRANSFORM` is part of single UPDATE statement
- WHERE clause includes `JSON_VALUE(auth_obj, '$.used') = 'false'`
- Only ONE data center can successfully update
- Second DC gets 0 rows affected (safe failure)

### 2. Database Timestamp (SYSTIMESTAMP)

✅ **Clock Skew Elimination**

```
DC1 Clock: 10:00:05 AM
DC2 Clock: 10:00:02 AM (3 seconds behind)
Database Clock: 10:00:04 AM (single source of truth)

All operations use: SYSTIMESTAMP
Result: Consistent expiration checks across all DCs
```

### 3. Idempotent DELETE Operations

✅ **Safe Concurrent Cleanup**

```sql
-- DC1 deletes expired token
DELETE FROM AUTHENTICATION_TOKENS WHERE next_expir_tmstp < SYSTIMESTAMP;
-- Deletes 5 rows

-- DC2 runs cleanup 2 seconds later
DELETE FROM AUTHENTICATION_TOKENS WHERE next_expir_tmstp < SYSTIMESTAMP;
-- Deletes 0 rows (already deleted by DC1)
-- No error thrown!
```

---

## Indexing Considerations

### Functional Index on JSON_VALUE (Optional but Recommended)

For better performance on JSON queries, create functional index:

```sql
-- Index on tokenValue extracted from JSON
CREATE INDEX idx_auth_obj_token_value
ON AUTHENTICATION_TOKENS(JSON_VALUE(auth_obj, '$.tokenValue'));

-- Index on used flag for cleanup queries
CREATE INDEX idx_auth_obj_used
ON AUTHENTICATION_TOKENS(JSON_VALUE(auth_obj, '$.used'), token_type);
```

**Performance Impact:**
- Without index: Full table scan + JSON parse per row
- With index: Direct lookup using indexed JSON value
- Recommended for production with high traffic

---

## Testing JSON Queries

### Manual Testing in SQL Developer

```sql
-- 1. Insert test token with JSON
INSERT INTO AUTHENTICATION_TOKENS (
    authentication_token_id,
    token_type,
    auth_obj,
    sys_id,
    expir_prod_in_min,
    next_expir_tmstp,
    row_crte_tmstp,
    row_lst_updt_tmstp
) VALUES (
    'test-token-001',
    'AUTHORIZATION_CODE',
    '{"tokenValue":"testCode123","used":false,"usedAt":null,"lastUsedAt":null}',
    'session-xyz',
    1,
    SYSTIMESTAMP + INTERVAL '1' MINUTE,
    SYSTIMESTAMP,
    SYSTIMESTAMP
);

-- 2. Query by token value
SELECT * FROM AUTHENTICATION_TOKENS
WHERE JSON_VALUE(auth_obj, '$.tokenValue') = 'testCode123';

-- 3. Mark as used (atomic update)
UPDATE AUTHENTICATION_TOKENS
SET auth_obj = JSON_TRANSFORM(auth_obj,
        SET '$.used' = 'true',
        SET '$.usedAt' = TO_CHAR(SYSTIMESTAMP, 'YYYY-MM-DD"T"HH24:MI:SS.FF3"Z"')),
    row_lst_updt_tmstp = SYSTIMESTAMP
WHERE JSON_VALUE(auth_obj, '$.tokenValue') = 'testCode123'
  AND (JSON_VALUE(auth_obj, '$.used') = 'false' OR JSON_VALUE(auth_obj, '$.used') IS NULL);

-- 4. Verify JSON was updated
SELECT
    authentication_token_id,
    JSON_VALUE(auth_obj, '$.tokenValue') as token_value,
    JSON_VALUE(auth_obj, '$.used') as is_used,
    JSON_VALUE(auth_obj, '$.usedAt') as used_at
FROM AUTHENTICATION_TOKENS
WHERE authentication_token_id = 'test-token-001';
```

---

## Troubleshooting

### Issue: JSON_VALUE returns NULL

**Symptoms:**
```sql
SELECT JSON_VALUE(auth_obj, '$.tokenValue') FROM AUTHENTICATION_TOKENS;
-- Returns: NULL
```

**Causes:**
1. `auth_obj` is not valid JSON
2. JSON path is incorrect
3. Field doesn't exist in JSON

**Solution:**
```sql
-- Check if auth_obj is valid JSON
SELECT auth_obj FROM AUTHENTICATION_TOKENS WHERE authentication_token_id = 'xyz';

-- Verify JSON structure
SELECT
    CASE
        WHEN JSON_VALUE(auth_obj, '$.tokenValue') IS NULL THEN 'Field not found or invalid JSON'
        ELSE 'Valid'
    END as status
FROM AUTHENTICATION_TOKENS;
```

### Issue: JSON_TRANSFORM not updating

**Symptoms:**
```sql
UPDATE ... SET auth_obj = JSON_TRANSFORM(...) WHERE ...;
-- Returns: 0 rows updated
```

**Causes:**
1. WHERE clause filtering out all rows
2. Incorrect JSON path in JSON_VALUE
3. Token already updated by another DC

**Solution:**
```sql
-- Check if token exists
SELECT * FROM AUTHENTICATION_TOKENS
WHERE JSON_VALUE(auth_obj, '$.tokenValue') = 'yourTokenValue';

-- Check current 'used' status
SELECT JSON_VALUE(auth_obj, '$.used') as used_status
FROM AUTHENTICATION_TOKENS
WHERE JSON_VALUE(auth_obj, '$.tokenValue') = 'yourTokenValue';
```

---

## Migration from Column-Based to JSON-Based

If you have existing data with `used_at` column:

```sql
-- Migrate existing data to JSON format
UPDATE AUTHENTICATION_TOKENS
SET auth_obj = JSON_OBJECT(
    'tokenValue' VALUE auth_obj,  -- Assume old auth_obj was token value
    'used' VALUE CASE WHEN used_at IS NOT NULL THEN 'true' ELSE 'false' END,
    'usedAt' VALUE TO_CHAR(used_at, 'YYYY-MM-DD"T"HH24:MI:SS.FF3"Z"'),
    'lastUsedAt' VALUE TO_CHAR(last_used_tmstp, 'YYYY-MM-DD"T"HH24:MI:SS.FF3"Z"')
)
WHERE /* ... your conditions ... */;

-- Then drop old columns if needed
ALTER TABLE AUTHENTICATION_TOKENS DROP COLUMN used_at;
ALTER TABLE AUTHENTICATION_TOKENS DROP COLUMN last_used_tmstp;
```

---

## Summary

| Aspect | Implementation |
|--------|----------------|
| **Storage** | JSON in CLOB (auth_obj column) |
| **Token Value** | Stored in JSON as `tokenValue` |
| **One-Time Use** | Tracked via JSON fields `used` and `usedAt` |
| **Last Usage** | Tracked via JSON field `lastUsedAt` |
| **Queries** | Use `JSON_VALUE()` to extract and filter |
| **Updates** | Use `JSON_TRANSFORM()` for atomic modifications |
| **Timestamps** | Use Oracle `SYSTIMESTAMP` for consistency |
| **Distributed Safe** | ✅ Yes - atomic operations, idempotent deletes |
| **Performance** | Consider functional indexes on JSON paths |

---

**Document Version:** 1.0
**Last Updated:** 2025-01-01
**Author:** System
