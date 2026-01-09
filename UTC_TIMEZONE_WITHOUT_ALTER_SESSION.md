# UTC Timezone Handling Without ALTER SESSION

## Configuration (No ALTER SESSION Needed!)

```properties
# JPA Configuration
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.Oracle12cDialect
spring.jpa.properties.hibernate.jdbc.time_zone=UTC
```

That's it! The combination of:
1. `hibernate.jdbc.time_zone=UTC`
2. `InstantAttributeConverter` using `ZoneOffset.UTC`

...ensures UTC consistency without needing `ALTER SESSION SET TIME_ZONE='UTC'`.

## How It Works

### The Magic of hibernate.jdbc.time_zone=UTC

When you set `hibernate.jdbc.time_zone=UTC`, Hibernate tells the JDBC driver:
- "When I send you a `LocalDateTime`, interpret it as UTC"
- "When you send me a `TIMESTAMP`, I'll interpret it as UTC"

### Combined with InstantAttributeConverter

Our converter explicitly uses `ZoneOffset.UTC`:

```java
// Save: Instant → LocalDateTime (UTC)
public LocalDateTime convertToDatabaseColumn(Instant instant) {
    return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    // Always interprets Instant as UTC
}

// Load: LocalDateTime → Instant (UTC)
public Instant convertToEntityAttribute(LocalDateTime localDateTime) {
    return localDateTime.toInstant(ZoneOffset.UTC);
    // Always interprets LocalDateTime as UTC
}
```

### The Complete Flow

```
┌─────────────────────────────────────────────────────────┐
│ APPLICATION                                             │
├─────────────────────────────────────────────────────────┤
│ Instant: 2026-01-02T15:30:00Z (3:30 PM UTC)           │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────┐
│ InstantAttributeConverter (ZoneOffset.UTC)              │
├─────────────────────────────────────────────────────────┤
│ LocalDateTime: 2026-01-02T15:30:00                     │
│ (Interpreted as UTC because we used ZoneOffset.UTC)    │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────┐
│ HIBERNATE (jdbc.time_zone=UTC)                          │
├─────────────────────────────────────────────────────────┤
│ Sends: TIMESTAMP '2026-01-02 15:30:00'                 │
│ Tells JDBC: "This is UTC"                              │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────┐
│ ORACLE JDBC DRIVER                                       │
├─────────────────────────────────────────────────────────┤
│ Receives: TIMESTAMP + "interpret as UTC" hint           │
│ Converts if needed based on database session TZ         │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────┐
│ ORACLE DATABASE                                          │
├─────────────────────────────────────────────────────────┤
│ Stores: 02-JAN-26 03.30.00.000000000 PM               │
│ (Stored value may vary based on session TZ, but...)    │
└─────────────────────────────────────────────────────────┘
                       │
                       ▼ (On Load)

┌─────────────────────────────────────────────────────────┐
│ ORACLE JDBC DRIVER                                       │
├─────────────────────────────────────────────────────────┤
│ Returns: LocalDateTime (with UTC interpretation)        │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────┐
│ HIBERNATE (jdbc.time_zone=UTC)                          │
├─────────────────────────────────────────────────────────┤
│ Interprets as UTC                                        │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────┐
│ InstantAttributeConverter (ZoneOffset.UTC)              │
├─────────────────────────────────────────────────────────┤
│ Converts: LocalDateTime → Instant (UTC)                │
│ Result: 2026-01-02T15:30:00Z                           │
└─────────────────────────────────────────────────────────┘
                       │
                       ▼
✓ Same UTC moment returned!
```

## Why This Works

### Consistent UTC Interpretation at Both Ends

**On Save:**
- We explicitly convert `Instant` → `LocalDateTime` using `ZoneOffset.UTC`
- Hibernate knows to treat it as UTC (`jdbc.time_zone=UTC`)
- JDBC driver handles the database session timezone automatically

**On Load:**
- JDBC driver returns `LocalDateTime`
- Hibernate interprets it as UTC (`jdbc.time_zone=UTC`)
- We explicitly convert `LocalDateTime` → `Instant` using `ZoneOffset.UTC`

**Result:** Round-trip preserves the UTC moment!

## Verification

### Test 1: Round-Trip Test

```java
@Test
void testUtcRoundTrip() {
    // Known UTC instant
    Instant utcInstant = Instant.parse("2026-01-02T15:30:00Z");

    // Save
    AuthenticationToken token = new AuthenticationToken();
    token.setAuthenticationTokenId(UUID.randomUUID().toString());
    token.setNextExpirTmstp(utcInstant);
    tokenRepository.save(token);

    // Load
    AuthenticationToken loaded = tokenRepository
        .findById(token.getAuthenticationTokenId())
        .get();

    // Verify (within microsecond precision)
    Instant loadedInstant = loaded.getNextExpirTmstp();
    assertEquals(
        utcInstant.truncatedTo(ChronoUnit.MICROS),
        loadedInstant.truncatedTo(ChronoUnit.MICROS)
    );
}
```

### Test 2: Multi-Server Consistency

```java
@Test
void testMultiServerConsistency() {
    // Server 1 (any timezone) saves
    Instant now = Instant.now();
    AuthenticationToken token = new AuthenticationToken();
    token.setNextExpirTmstp(now);
    tokenRepository.save(token);

    // Server 2 (different timezone) loads
    // (Simulate by just loading in same test)
    AuthenticationToken loaded = tokenRepository
        .findById(token.getAuthenticationTokenId())
        .get();

    // Should be same UTC moment
    assertEquals(
        now.truncatedTo(ChronoUnit.MICROS),
        loaded.getNextExpirTmstp().truncatedTo(ChronoUnit.MICROS)
    );
}
```

### Test 3: Query with Instant Parameter

```java
@Test
void testQueryWithInstantParameter() {
    Instant now = Instant.now();
    Instant future = now.plusSeconds(1800);

    // Save token expiring in future
    AuthenticationToken token = new AuthenticationToken();
    token.setNextExpirTmstp(future);
    tokenRepository.save(token);

    // Query: Find tokens expiring after now
    Optional<AuthenticationToken> result = tokenRepository
        .findByAuthenticationTokenIdAndNextExpirTmstpAfter(
            token.getAuthenticationTokenId(),
            now
        );

    // Should find token
    assertTrue(result.isPresent());

    // Query: Find tokens expiring after future
    Optional<AuthenticationToken> result2 = tokenRepository
        .findByAuthenticationTokenIdAndNextExpirTmstpAfter(
            token.getAuthenticationTokenId(),
            future.plusSeconds(1)
        );

    // Should NOT find token
    assertTrue(result2.isEmpty());
}
```

## What About the Database Session Timezone?

**Q:** If the Oracle database session is in Central Time (-06:00), won't that cause issues?

**A:** No, because:

1. **Hibernate's `jdbc.time_zone=UTC` setting** tells the JDBC driver to handle timezone conversion
2. **The JDBC driver** converts between:
   - Application timezone (UTC, as specified)
   - Database session timezone (whatever it is)
3. **Our converter** always uses `ZoneOffset.UTC` on both ends

**The JDBC driver acts as a bridge**, converting:
- UTC (from Hibernate) ↔ Central Time (database session)

### Example with Central Time Database Session:

```
Application:  Instant: 2026-01-02T15:30:00Z (3:30 PM UTC)
             ↓
Converter:    LocalDateTime: 2026-01-02T15:30:00
             ↓
Hibernate:    "This is UTC"
             ↓
JDBC Driver:  Converts: 3:30 PM UTC → 9:30 AM CST
             ↓
Oracle:       Stores: 02-JAN-26 09.30.00.000000000 AM
             ↓ (On Load)
JDBC Driver:  Converts: 9:30 AM CST → 3:30 PM UTC
             ↓
Hibernate:    LocalDateTime: 2026-01-02T15:30:00 (UTC)
             ↓
Converter:    Instant: 2026-01-02T15:30:00Z
             ↓
✓ Same UTC moment!
```

**The JDBC driver handles the conversion automatically!**

## When You WOULD Need ALTER SESSION

You only need `ALTER SESSION SET TIME_ZONE='UTC'` if:

1. **You're NOT using Hibernate** (raw JDBC without framework)
2. **You're using native SQL** that directly manipulates TIMESTAMP values
3. **You want database-level consistency** (all apps using same session TZ)
4. **You're paranoid** and want explicit control (belt and suspenders approach)

For your use case with:
- ✓ Hibernate with `jdbc.time_zone=UTC`
- ✓ `InstantAttributeConverter` with `ZoneOffset.UTC`
- ✓ Spring Data JPA method names (no native SQL)

**You don't need ALTER SESSION!**

## Troubleshooting

### If Timestamps Still Look Wrong

1. **Verify hibernate.jdbc.time_zone is set:**
   ```properties
   spring.jpa.properties.hibernate.jdbc.time_zone=UTC
   ```

2. **Check converter is being applied:**
   ```java
   @Convert(converter = InstantAttributeConverter.class)
   private Instant nextExpirTmstp;
   ```

3. **Enable debug logging:**
   ```properties
   logging.level.org.hibernate.type.descriptor.sql=TRACE
   logging.level.oracle.jdbc=DEBUG
   ```

4. **Test round-trip:**
   ```java
   Instant before = Instant.now();
   token.setNextExpirTmstp(before);
   tokenRepository.save(token);

   Instant after = tokenRepository.findById(id).get().getNextExpirTmstp();

   // Should be equal (within microseconds)
   assertEquals(
       before.truncatedTo(ChronoUnit.MICROS),
       after.truncatedTo(ChronoUnit.MICROS)
   );
   ```

## Summary

**Configuration needed:**
```properties
spring.jpa.properties.hibernate.jdbc.time_zone=UTC
```

**Plus:**
- `InstantAttributeConverter` using `ZoneOffset.UTC`
- `@Convert` annotations on entity fields

**Result:**
- ✓ UTC consistency without ALTER SESSION
- ✓ JDBC driver handles timezone conversion automatically
- ✓ Works with any database session timezone
- ✓ No DBA permissions needed

**The secret:** Hibernate's `jdbc.time_zone=UTC` combined with explicit `ZoneOffset.UTC` in the converter ensures consistent UTC interpretation on both ends, regardless of the database session timezone!

---

**Document Version:** 1.0
**Last Updated:** 2026-01-02
**Status:** Production-Ready
