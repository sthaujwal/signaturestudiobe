# Implementation Summary: CLOB-Based JSON Authentication Tokens

## What Was Changed

Based on your feedback that **"there is no used_at column on Authentication_token table. The AuthObj is a CLOB field"**, I completely redesigned the implementation to use JSON metadata stored in the `auth_obj` CLOB field.

---

## Files Modified

### 1. [AuthenticationToken.java](src/main/java/com/wellsfargo/signaturestudio/domain/AuthenticationToken.java)

**Changes:**
- Changed `auth_obj` from `VARCHAR2(128)` to `@Lob` (CLOB)
- Removed `usedAt` and `lastUsedTmstp` columns (don't exist in your schema)
- Added `TokenMetadata` inner class for JSON structure
- Added JSON serialization/deserialization using Jackson
- Added `@PostLoad` hook to parse JSON after loading from database
- Token value now stored in JSON: `{"tokenValue": "...", "used": false, "usedAt": null, "lastUsedAt": null}`

**Key Methods:**
```java
public String getTokenValue()  // Extracts tokenValue from JSON
public void setTokenValue(String tokenValue)  // Updates JSON with new token value
public boolean isUsed()  // Checks 'used' field in JSON
public void markAsUsed()  // Updates JSON to set used=true, usedAt=timestamp
public void extendExpiration(int validityMinutes)  // Updates JSON with lastUsedAt
```

### 2. [AuthenticationTokenRepository.java](src/main/java/com/wellsfargo/signaturestudio/repository/AuthenticationTokenRepository.java)

**Changes:**
- All queries now use Oracle JSON functions (`JSON_VALUE`, `JSON_TRANSFORM`)
- `findValidTokenByValue()` - Uses `JSON_VALUE(auth_obj, '$.tokenValue')` to extract token
- `markAuthorizationCodeAsUsed()` - Uses `JSON_TRANSFORM` to atomically set `used=true` and `usedAt`
- `extendAccessTokenExpiration()` - Uses `JSON_TRANSFORM` to atomically update `lastUsedAt`
- `deleteOldUsedAuthorizationCodes()` - Uses `JSON_VALUE` to check `used` field and `usedAt` timestamp

**Oracle JSON Functions Used:**
```sql
-- Extract field from JSON
JSON_VALUE(auth_obj, '$.tokenValue')
JSON_VALUE(auth_obj, '$.used')

-- Update JSON field atomically
JSON_TRANSFORM(auth_obj,
    SET '$.used' = 'true',
    SET '$.usedAt' = TO_CHAR(SYSTIMESTAMP, 'YYYY-MM-DD"T"HH24:MI:SS.FF3"Z"')
)

-- Parse timestamp from JSON
TO_TIMESTAMP(JSON_VALUE(auth_obj, '$.usedAt'), 'YYYY-MM-DD"T"HH24:MI:SS.FF3"Z"')
```

### 3. [AuthenticationTokenService.java](src/main/java/com/wellsfargo/signaturestudio/service/AuthenticationTokenService.java)

**Changes:**
- `generateToken()` now calls `token.setTokenValue()` instead of `token.setAuthObj()`
- Service methods remain the same (uses updated repository methods)
- All atomic operations now work with JSON metadata
- Comments updated to reflect JSON-based approach

---

## JSON Structure in auth_obj CLOB

```json
{
  "tokenValue": "eJxVkF2PgjAUhu_5FU3vm6K4q...",
  "used": false,
  "usedAt": null,
  "lastUsedAt": null
}
```

**Field Mapping:**

| Field | Type | Purpose | Token Type |
|-------|------|---------|------------|
| `tokenValue` | String | Actual token (Base64 random bytes) | Both |
| `used` | Boolean | Whether code was consumed | AUTHORIZATION_CODE |
| `usedAt` | ISO 8601 String | When code was used | AUTHORIZATION_CODE |
| `lastUsedAt` | ISO 8601 String | Last API request time | ACCESS_TOKEN |

---

## Database Schema Requirements

Your existing AUTHENTICATION_TOKENS table should have:

```sql
CREATE TABLE AUTHENTICATION_TOKENS (
    authentication_token_id VARCHAR2(64) PRIMARY KEY,
    token_type VARCHAR2(20) NOT NULL,
    auth_obj CLOB NOT NULL,  -- JSON metadata
    sys_id VARCHAR2(255) NOT NULL,  -- Session ID FK
    expir_prod_in_min NUMBER(10) NOT NULL,
    next_expir_tmstp TIMESTAMP NOT NULL,
    row_crte_tmstp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    row_lst_updt_tmstp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT fk_auth_token_session FOREIGN KEY (sys_id)
        REFERENCES SPRING_SESSION(SESSION_ID) ON DELETE CASCADE
);
```

**No migration needed** if your schema already has `auth_obj` as CLOB.

---

## How It Works

### Token Generation

```java
String tokenValue = generateAuthorizationCode(sessionId);
```

**Database INSERT:**
```sql
INSERT INTO AUTHENTICATION_TOKENS (
    authentication_token_id,
    token_type,
    auth_obj,  -- CLOB with JSON
    sys_id,
    expir_prod_in_min,
    next_expir_tmstp
) VALUES (
    'uuid-123',
    'AUTHORIZATION_CODE',
    '{"tokenValue":"abc123xyz","used":false,"usedAt":null,"lastUsedAt":null}',
    'session-xyz',
    1,
    SYSTIMESTAMP + INTERVAL '1' MINUTE
);
```

### Token Validation (Authorization Code)

```java
Optional<String> sessionId = validateAndConsumeAuthorizationCode(codeValue);
```

**Atomic UPDATE (prevents race conditions):**
```sql
UPDATE AUTHENTICATION_TOKENS
SET auth_obj = JSON_TRANSFORM(auth_obj,
        SET '$.used' = 'true',
        SET '$.usedAt' = TO_CHAR(SYSTIMESTAMP, 'YYYY-MM-DD"T"HH24:MI:SS.FF3"Z"')),
    row_lst_updt_tmstp = SYSTIMESTAMP
WHERE JSON_VALUE(auth_obj, '$.tokenValue') = 'abc123xyz'
  AND token_type = 'AUTHORIZATION_CODE'
  AND (JSON_VALUE(auth_obj, '$.used') = 'false' OR JSON_VALUE(auth_obj, '$.used') IS NULL)
  AND next_expir_tmstp > SYSTIMESTAMP;
-- Returns: 1 if successful, 0 if already used/expired
```

**JSON Before:**
```json
{"tokenValue":"abc123xyz","used":false,"usedAt":null}
```

**JSON After:**
```json
{"tokenValue":"abc123xyz","used":true,"usedAt":"2025-01-01T10:05:00.123Z"}
```

### Token Validation (Access Token)

```java
Optional<String> sessionId = validateAndExtendAccessToken(tokenValue);
```

**Atomic UPDATE (extends expiration):**
```sql
UPDATE AUTHENTICATION_TOKENS
SET next_expir_tmstp = SYSTIMESTAMP + NUMTODSINTERVAL(30, 'MINUTE'),
    auth_obj = JSON_TRANSFORM(auth_obj,
        SET '$.lastUsedAt' = TO_CHAR(SYSTIMESTAMP, 'YYYY-MM-DD"T"HH24:MI:SS.FF3"Z"')),
    row_lst_updt_tmstp = SYSTIMESTAMP
WHERE JSON_VALUE(auth_obj, '$.tokenValue') = 'xyz789'
  AND token_type = 'ACCESS_TOKEN'
  AND next_expir_tmstp > SYSTIMESTAMP;
-- Returns: 1 if successful, 0 if expired/not found
```

**JSON Before:**
```json
{"tokenValue":"xyz789","lastUsedAt":"2025-01-01T10:00:00.000Z"}
```

**JSON After:**
```json
{"tokenValue":"xyz789","lastUsedAt":"2025-01-01T10:15:30.456Z"}
```

---

## Distributed System Safety

### ✅ Atomic Operations

**Problem:** Two data centers try to consume same authorization code simultaneously.

**Solution:** `JSON_TRANSFORM` in UPDATE with conditional WHERE clause.

```
Timeline:
T+0ms   DC1: UPDATE ... WHERE used=false → Success (1 row)
T+1ms   DC2: UPDATE ... WHERE used=false → Failure (0 rows, already used=true)

Result: Only DC1 successfully consumes code. DC2 returns "already used" error.
```

### ✅ Clock Skew Elimination

**Problem:** DC1 and DC2 have different system clocks (time zones, clock drift).

**Solution:** Use Oracle `SYSTIMESTAMP` as single source of truth.

```
DC1 Clock: 10:00:05 AM
DC2 Clock: 10:00:02 AM (3 seconds behind)
Database:  10:00:04 AM (single source of truth)

All queries use: SYSTIMESTAMP
Result: Consistent expiration checks across all DCs
```

### ✅ Idempotent Operations

**Problem:** Multiple DCs run cleanup task simultaneously.

**Solution:** DELETE operations are idempotent (safe to run multiple times).

```sql
-- DC1 deletes expired tokens
DELETE FROM AUTHENTICATION_TOKENS WHERE next_expir_tmstp < SYSTIMESTAMP;
-- Deletes 5 rows

-- DC2 runs cleanup 2 seconds later
DELETE FROM AUTHENTICATION_TOKENS WHERE next_expir_tmstp < SYSTIMESTAMP;
-- Deletes 0 rows (already deleted)
-- No error! Safe.
```

---

## Testing

### Unit Test Example

```java
@Test
void testJsonMetadataSerialization() {
    AuthenticationToken token = new AuthenticationToken();
    token.setTokenValue("test123");

    // Save to database (JSON serialization happens automatically)
    tokenRepository.save(token);

    // Load from database (JSON deserialization happens automatically)
    AuthenticationToken loaded = tokenRepository.findById(token.getAuthenticationTokenId()).get();

    assertEquals("test123", loaded.getTokenValue());
    assertFalse(loaded.isUsed());
}

@Test
void testMarkAuthorizationCodeAsUsed() {
    // Generate code
    String code = tokenService.generateAuthorizationCode("session-123");

    // First consumption - should succeed
    Optional<String> sessionId1 = tokenService.validateAndConsumeAuthorizationCode(code);
    assertTrue(sessionId1.isPresent());
    assertEquals("session-123", sessionId1.get());

    // Second consumption - should fail (already used)
    Optional<String> sessionId2 = tokenService.validateAndConsumeAuthorizationCode(code);
    assertFalse(sessionId2.isPresent());
}
```

### Manual SQL Testing

```sql
-- 1. Insert test token
INSERT INTO AUTHENTICATION_TOKENS (
    authentication_token_id, token_type, auth_obj, sys_id,
    expir_prod_in_min, next_expir_tmstp, row_crte_tmstp, row_lst_updt_tmstp
) VALUES (
    'test-001', 'AUTHORIZATION_CODE',
    '{"tokenValue":"testCode123","used":false,"usedAt":null}',
    'session-xyz', 1, SYSTIMESTAMP + INTERVAL '5' MINUTE,
    SYSTIMESTAMP, SYSTIMESTAMP
);

-- 2. Query by token value
SELECT * FROM AUTHENTICATION_TOKENS
WHERE JSON_VALUE(auth_obj, '$.tokenValue') = 'testCode123';

-- 3. Mark as used (test atomic operation)
UPDATE AUTHENTICATION_TOKENS
SET auth_obj = JSON_TRANSFORM(auth_obj,
        SET '$.used' = 'true',
        SET '$.usedAt' = TO_CHAR(SYSTIMESTAMP, 'YYYY-MM-DD"T"HH24:MI:SS.FF3"Z"')),
    row_lst_updt_tmstp = SYSTIMESTAMP
WHERE JSON_VALUE(auth_obj, '$.tokenValue') = 'testCode123'
  AND (JSON_VALUE(auth_obj, '$.used') = 'false' OR JSON_VALUE(auth_obj, '$.used') IS NULL);
-- Should return: 1 row updated

-- 4. Try marking as used again (test idempotency)
-- Same UPDATE as step 3
-- Should return: 0 rows updated (already used=true)

-- 5. Verify JSON was updated
SELECT
    authentication_token_id,
    JSON_VALUE(auth_obj, '$.tokenValue') as token,
    JSON_VALUE(auth_obj, '$.used') as is_used,
    JSON_VALUE(auth_obj, '$.usedAt') as used_at
FROM AUTHENTICATION_TOKENS
WHERE authentication_token_id = 'test-001';
```

---

## Performance Considerations

### Functional Indexes (Recommended for Production)

```sql
-- Index on tokenValue for fast lookups
CREATE INDEX idx_auth_obj_token_value
ON AUTHENTICATION_TOKENS(JSON_VALUE(auth_obj, '$.tokenValue'));

-- Index on used flag for cleanup queries
CREATE INDEX idx_auth_obj_used_type
ON AUTHENTICATION_TOKENS(JSON_VALUE(auth_obj, '$.used'), token_type);
```

**Without indexes:**
- Full table scan + JSON parsing on every row
- Slow for large tables (>10,000 rows)

**With functional indexes:**
- Direct lookup using indexed JSON value
- Fast even with millions of rows
- Recommended for production

---

## Migration Notes

If you already have data in the table with a different schema:

### Option 1: Table is Empty
No migration needed! Just deploy the new code.

### Option 2: Table Has Data (auth_obj is VARCHAR2)

```sql
-- Backup table first!
CREATE TABLE AUTHENTICATION_TOKENS_BACKUP AS SELECT * FROM AUTHENTICATION_TOKENS;

-- Convert VARCHAR2 auth_obj to JSON format
UPDATE AUTHENTICATION_TOKENS
SET auth_obj = JSON_OBJECT(
    'tokenValue' VALUE auth_obj,
    'used' VALUE 'false',
    'usedAt' VALUE NULL,
    'lastUsedAt' VALUE NULL
);

-- Change column type to CLOB (if needed)
ALTER TABLE AUTHENTICATION_TOKENS MODIFY auth_obj CLOB;
```

### Option 3: Clean Slate

```sql
-- Delete all existing tokens (sessions will create new ones)
TRUNCATE TABLE AUTHENTICATION_TOKENS;

-- Deploy new code
-- Tokens will be created with correct JSON format
```

---

## Troubleshooting

### Issue: JSON_VALUE returns NULL

**Cause:** Invalid JSON or missing field.

**Solution:**
```sql
-- Check raw JSON
SELECT auth_obj FROM AUTHENTICATION_TOKENS WHERE authentication_token_id = 'xyz';

-- Validate JSON
SELECT
    CASE
        WHEN JSON_VALUE(auth_obj, '$.tokenValue') IS NULL THEN 'Invalid or missing field'
        ELSE 'Valid'
    END as status
FROM AUTHENTICATION_TOKENS;
```

### Issue: "ORA-40441: JSON syntax error"

**Cause:** Malformed JSON in auth_obj CLOB.

**Solution:**
```sql
-- Find malformed JSON
SELECT authentication_token_id, auth_obj
FROM AUTHENTICATION_TOKENS
WHERE NOT JSON_EXISTS(auth_obj, '$');

-- Fix by regenerating JSON
UPDATE AUTHENTICATION_TOKENS
SET auth_obj = '{"tokenValue":"placeholder","used":false,"usedAt":null}'
WHERE authentication_token_id = 'problematic-id';
```

---

## Documentation

Complete implementation guides:

1. **[CLOB_JSON_IMPLEMENTATION.md](CLOB_JSON_IMPLEMENTATION.md)** - Detailed JSON implementation guide
2. **[AUTHENTICATION_IMPLEMENTATION.md](AUTHENTICATION_IMPLEMENTATION.md)** - Full authentication flow
3. **[DISTRIBUTED_SYSTEM_GUIDE.md](DISTRIBUTED_SYSTEM_GUIDE.md)** - Multi-DC deployment guide
4. **[SESSION_EVENT_DISTRIBUTED_SYSTEM.md](SESSION_EVENT_DISTRIBUTED_SYSTEM.md)** - SessionEventListener analysis

---

## Next Steps

### 1. Review Changes
- Check [AuthenticationToken.java](src/main/java/com/wellsfargo/signaturestudio/domain/AuthenticationToken.java)
- Review [AuthenticationTokenRepository.java](src/main/java/com/wellsfargo/signaturestudio/repository/AuthenticationTokenRepository.java)
- Verify [AuthenticationTokenService.java](src/main/java/com/wellsfargo/signaturestudio/service/AuthenticationTokenService.java)

### 2. Test Locally
```bash
# Compile
./gradlew clean build

# Run tests
./gradlew test

# Test SQL queries manually
sqlplus user/password@database
@test_json_queries.sql
```

### 3. Deploy to Development
- Deploy to dev environment
- Test token generation
- Test code exchange flow
- Test token extension
- Test multi-DC behavior

### 4. Production Deployment
- Add functional indexes (see above)
- Deploy to production
- Monitor logs for JSON parsing errors
- Watch cleanup task performance

---

## Summary

✅ **Redesigned** - Entity uses CLOB with JSON metadata
✅ **Atomic Operations** - JSON_TRANSFORM prevents race conditions
✅ **Clock Skew Fixed** - SYSTIMESTAMP as single source of truth
✅ **Distributed Safe** - Works across multiple data centers
✅ **No Schema Changes** - Works with your existing CLOB column
✅ **Fully Documented** - Complete implementation guides provided

**The implementation now matches your actual database schema!**

---

**Document Version:** 1.0
**Last Updated:** 2025-01-01
**Author:** System
