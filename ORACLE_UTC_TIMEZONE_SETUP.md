# Oracle UTC Timezone Configuration

## The Problem

When you saw timestamps stored as `02-JAN-26 09.19.23.874708000 PM`, Oracle was storing them in **Central Time** instead of UTC.

### Why This Happened

Oracle's `TIMESTAMP` column doesn't store timezone information. When you insert a value, Oracle interprets it according to the **session timezone**.

**Without UTC configuration:**
```
Application → LocalDateTime(2026-01-02T15:30:00)
              ↓
Oracle Session (Central Time -06:00)
              ↓
Interprets as: 2026-01-02 15:30:00 CST
              ↓
Stores: 02-JAN-26 03.30.00.000000000 PM (3:30 PM CST)
              ↓
When converting back to UTC: 2026-01-02T21:30:00Z
              ↓
❌ WRONG! Off by 6 hours!
```

## The Solution

Set Oracle session timezone to UTC for your application's database connections.

### Configuration Added

**File:** `application.properties`

```properties
# Set Oracle session timezone to UTC (ensures database stores values in UTC)
spring.datasource.hikari.connection-init-sql=ALTER SESSION SET TIME_ZONE='UTC'
```

### How It Works

**With UTC configuration:**
```
Application → LocalDateTime(2026-01-02T15:30:00)
              ↓
Oracle Session (UTC +00:00) ← Set by connection-init-sql
              ↓
Interprets as: 2026-01-02 15:30:00 UTC
              ↓
Stores: 02-JAN-26 03.30.00.000000000 PM (3:30 PM UTC)
              ↓
When converting back to UTC: 2026-01-02T15:30:00Z
              ↓
✓ CORRECT! Same UTC moment preserved!
```

## What This Configuration Does

1. **Connection Initialization**
   - HikariCP creates a new database connection
   - Runs: `ALTER SESSION SET TIME_ZONE='UTC'`
   - Sets the timezone for this specific connection only

2. **Scope**
   - Applies to all connections in your application's connection pool
   - Does NOT affect other applications using the same database
   - Does NOT change the database's global timezone setting

3. **Persistence**
   - Setting persists for the lifetime of the connection
   - When connection is returned to pool, timezone remains UTC
   - New connections automatically get UTC timezone via init-sql

## Verification

### Automatic Verification on Startup

The `DatabaseTimezoneVerifier` component runs at startup and logs:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Oracle Database Timezone Configuration:
  Session Timezone: +00:00 ✓ (UTC)
  Database Timezone: -06:00 (global database setting)
✓ Oracle session timezone correctly set to UTC
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### Manual Verification via SQL

Connect to your database and run:

```sql
-- Show current session timezone
SELECT SESSIONTIMEZONE FROM DUAL;
-- Expected: +00:00

-- Show database timezone (global setting)
SELECT DBTIMEZONE FROM DUAL;
-- May be different (e.g., -06:00) - that's OK!
```

**Important:** The **session timezone** must be `+00:00`. The database timezone can be anything.

## How Timestamps are Now Stored

### Example: Saving a Token

```java
// Your code (any server timezone):
Instant instant = Instant.now();
// → 2026-01-02T15:30:00Z (3:30 PM UTC)

// InstantAttributeConverter:
LocalDateTime ldt = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
// → 2026-01-02T15:30:00 (no timezone)

// Hibernate sends to Oracle:
// → INSERT ... VALUES (..., TIMESTAMP '2026-01-02 15:30:00', ...)

// Oracle session timezone = UTC:
// → Interprets as 3:30 PM UTC

// Oracle stores:
// → 02-JAN-26 03.30.00.000000000 PM
// → This represents 3:30 PM UTC ✓
```

### Example: Loading a Token

```sql
-- Oracle returns:
-- 02-JAN-26 03.30.00.000000000 PM

-- JDBC provides:
-- LocalDateTime: 2026-01-02T15:30:00

-- InstantAttributeConverter:
instant = localDateTime.toInstant(ZoneOffset.UTC)
-- → 2026-01-02T15:30:00Z

-- Your code gets:
-- ✓ Instant: 2026-01-02T15:30:00Z (exact same UTC moment!)
```

## Multi-Datacenter Consistency

**All servers now store/read UTC correctly:**

| Server Location | Server Timezone | Oracle Session TZ | Stored Value | Loaded Value |
|-----------------|-----------------|-------------------|--------------|--------------|
| New York | EST (UTC-5) | **UTC** | 02-JAN-26 03.30.00 PM | 2026-01-02T15:30:00Z |
| London | GMT (UTC+0) | **UTC** | 02-JAN-26 03.30.00 PM | 2026-01-02T15:30:00Z |
| Tokyo | JST (UTC+9) | **UTC** | 02-JAN-26 03.30.00 PM | 2026-01-02T15:30:00Z |

**All servers see the same UTC moment** ✓

## Comparing: Before vs After

### Before Configuration (WRONG ❌)

**Server in Central Time:**
```
Instant.now() → 2026-01-02T15:30:00Z (3:30 PM UTC)
              ↓
Oracle Session TZ: -06:00 (Central Time)
              ↓
Stored: 02-JAN-26 03.30.00 PM (3:30 PM CST = 9:30 PM UTC)
              ↓
Loaded: 2026-01-02T21:30:00Z
              ↓
❌ OFF BY 6 HOURS!
```

### After Configuration (CORRECT ✓)

**Server in Central Time:**
```
Instant.now() → 2026-01-02T15:30:00Z (3:30 PM UTC)
              ↓
Oracle Session TZ: +00:00 (UTC)
              ↓
Stored: 02-JAN-26 03.30.00 PM (3:30 PM UTC)
              ↓
Loaded: 2026-01-02T15:30:00Z
              ↓
✓ EXACT SAME UTC MOMENT!
```

## Why We Need Both Settings

You might notice we have TWO timezone configurations:

### 1. hibernate.jdbc.time_zone=UTC
```properties
spring.jpa.properties.hibernate.jdbc.time_zone=UTC
```
**Purpose:** Tells Hibernate to interpret database timestamps as UTC when converting to Java types.

### 2. connection-init-sql (Oracle Session Timezone)
```properties
spring.datasource.hikari.connection-init-sql=ALTER SESSION SET TIME_ZONE='UTC'
```
**Purpose:** Tells Oracle to interpret incoming timestamps as UTC when storing.

**Both are needed:**
- Without #1: Hibernate might interpret timestamps as server local time
- Without #2: Oracle interprets timestamps as database/session local time
- **With both:** Consistent UTC throughout the entire stack ✓

## Complete Flow: Save → Store → Load

```
┌─────────────────────────────────────────────────────────────────┐
│ APPLICATION LAYER                                               │
├─────────────────────────────────────────────────────────────────┤
│ Java: Instant.now()                                             │
│ Value: 2026-01-02T15:30:00Z (3:30 PM UTC)                      │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│ CONVERTER LAYER (InstantAttributeConverter)                     │
├─────────────────────────────────────────────────────────────────┤
│ Convert: Instant → LocalDateTime (ZoneOffset.UTC)              │
│ Value: 2026-01-02T15:30:00 (timezone-naive)                    │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│ HIBERNATE LAYER (hibernate.jdbc.time_zone=UTC)                 │
├─────────────────────────────────────────────────────────────────┤
│ Sends: TIMESTAMP '2026-01-02 15:30:00'                         │
│ Interprets as UTC per hibernate.jdbc.time_zone setting         │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│ ORACLE DATABASE (connection-init-sql: SET TIME_ZONE='UTC')     │
├─────────────────────────────────────────────────────────────────┤
│ Receives: TIMESTAMP '2026-01-02 15:30:00'                      │
│ Session TZ: +00:00 (UTC)                                        │
│ Interprets as: 3:30 PM UTC                                      │
│ Stores: 02-JAN-26 03.30.00.000000000 PM                        │
└─────────────────────────────────────────────────────────────────┘
```

## Testing

### Test 1: Save and Load Instant

```java
@Test
void testInstantStoredInUtc() {
    // Create token with known UTC time
    Instant utcTime = Instant.parse("2026-01-02T15:30:00Z");  // 3:30 PM UTC

    AuthenticationToken token = new AuthenticationToken();
    token.setAuthenticationTokenId(UUID.randomUUID().toString());
    token.setTokenType(TokenType.ACCESS_TOKEN);
    token.setSysId("test-session");
    token.setAuthObj("test-session");
    token.setExpirProdInMin(30);
    token.setNextExpirTmstp(utcTime);

    tokenRepository.save(token);

    // Load and verify
    AuthenticationToken loaded = tokenRepository.findById(token.getAuthenticationTokenId()).get();

    // Should match exactly (within microsecond precision)
    assertEquals(utcTime.truncatedTo(ChronoUnit.MICROS),
                 loaded.getNextExpirTmstp().truncatedTo(ChronoUnit.MICROS));
}
```

### Test 2: Query with Instant Parameter

```java
@Test
void testQueryWithInstantParameter() {
    Instant now = Instant.now();
    Instant future = now.plusSeconds(1800);

    // Create token expiring in future
    AuthenticationToken token = new AuthenticationToken();
    token.setNextExpirTmstp(future);
    tokenRepository.save(token);

    // Query using Instant parameter
    Optional<AuthenticationToken> result = tokenRepository
        .findByAuthenticationTokenIdAndNextExpirTmstpAfter(
            token.getAuthenticationTokenId(),
            now  // Should find token (expires after now)
        );

    assertTrue(result.isPresent());
}
```

### Test 3: Verify Database Storage (SQL)

```sql
-- Check what's actually stored in Oracle
SELECT
    authentication_token_id,
    next_expir_tmstp,
    TO_CHAR(next_expir_tmstp, 'YYYY-MM-DD HH24:MI:SS TZH:TZM') AS formatted_timestamp
FROM AUTHENTICATION_TOKEN
ORDER BY row_crte_tmstp DESC
FETCH FIRST 5 ROWS ONLY;

-- If timezone shows +00:00 or no TZ offset, it's UTC ✓
-- If timezone shows -06:00 or other offset, it's NOT UTC ❌
```

## Troubleshooting

### Issue: Still seeing non-UTC timestamps

**Symptoms:**
```
DatabaseTimezoneVerifier: Session Timezone: -06:00 ⚠ (NOT UTC!)
```

**Solutions:**

1. **Check connection-init-sql syntax:**
   ```properties
   # Correct:
   spring.datasource.hikari.connection-init-sql=ALTER SESSION SET TIME_ZONE='UTC'

   # Wrong (missing quotes):
   spring.datasource.hikari.connection-init-sql=ALTER SESSION SET TIME_ZONE=UTC
   ```

2. **Verify HikariCP is using the setting:**
   ```java
   // Enable HikariCP logging
   logging.level.com.zaxxer.hikari=DEBUG
   ```

3. **Check for connection pool override:**
   - Ensure no other configuration is overriding the connection-init-sql
   - Check for external connection pool configuration

4. **Test manually in Oracle:**
   ```sql
   -- Connect and run:
   ALTER SESSION SET TIME_ZONE='UTC';
   SELECT SESSIONTIMEZONE FROM DUAL;
   -- Should return: +00:00
   ```

### Issue: Different timestamps in different environments

**Cause:** Different Oracle session timezones across environments.

**Solution:** Ensure `connection-init-sql` is set in ALL environments (dev, staging, prod).

## Summary

**Configuration added:**
```properties
spring.datasource.hikari.connection-init-sql=ALTER SESSION SET TIME_ZONE='UTC'
```

**What it does:**
- Sets Oracle session timezone to UTC for your app's connections
- Ensures all timestamps are stored and interpreted as UTC
- Works with multi-datacenter deployments
- Does not affect other applications

**Result:**
- ✓ Timestamps stored in UTC
- ✓ Consistent across all servers
- ✓ Works with `Instant` + `LocalDateTime` converter
- ✓ No timezone conversion bugs

---

**Document Version:** 1.0
**Last Updated:** 2026-01-02
**Status:** Production-Ready
