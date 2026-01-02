# Manual AttributeConverter Approach

## Why Manual Application is Better

### Advantages ✅

1. **Explicit and Discoverable**
   - Developers can see `@Convert` annotation directly on fields
   - No hidden "magic" behavior
   - Easy to understand what's being converted

2. **Controlled Scope**
   - Only affects database entity classes
   - DTOs, request/response objects, and service layer remain unaffected
   - No surprise conversions in unexpected places

3. **Safer for Refactoring**
   - Won't accidentally convert new `Instant` fields
   - Changes require explicit annotation addition
   - Prevents bugs from implicit behavior

4. **Better for Testing**
   - Easier to mock entities without converter side effects
   - Test data is more predictable
   - Can test converter independently

5. **Clear Intent**
   - Shows developers: "This field needs special Oracle handling"
   - Makes database-specific concerns visible
   - Documents the technical decision

### Disadvantages of `autoApply = true` ❌

- Converts ALL `Instant` fields everywhere (including DTOs)
- Hidden behavior not obvious from reading code
- Can cause issues with fields you don't want converted
- May interfere with JSON serialization or other frameworks
- Global side effects from a single annotation

---

## Implementation

### 1. Converter Class (No autoApply)

**File:** `src/main/java/com/wellsfargo/signaturestudio/config/InstantAttributeConverter.java`

```java
@Converter  // No autoApply parameter!
public class InstantAttributeConverter implements AttributeConverter<Instant, LocalDateTime> {

    @Override
    public LocalDateTime convertToDatabaseColumn(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    @Override
    public Instant convertToEntityAttribute(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.toInstant(ZoneOffset.UTC);
    }
}
```

### 2. Entity Fields (Explicit @Convert)

**File:** `src/main/java/com/wellsfargo/signaturestudio/domain/AuthenticationToken.java`

```java
package com.wellsfargo.signaturestudio.domain;

import com.wellsfargo.signaturestudio.config.InstantAttributeConverter;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "AUTHENTICATION_TOKEN")
public class AuthenticationToken {

    // String fields - no converter needed
    @Column(name = "authentication_token_id", length = 64)
    private String authenticationTokenId;

    // Instant fields - explicit converter application
    @Column(name = "next_expir_tmstp", nullable = false)
    @Convert(converter = InstantAttributeConverter.class)  // ← Explicit!
    private Instant nextExpirTmstp;

    @Column(name = "row_crte_tmstp", nullable = false, updatable = false)
    @Convert(converter = InstantAttributeConverter.class)  // ← Explicit!
    private Instant rowCrteTmstp;

    @Column(name = "row_lst_updt_tmstp", nullable = false)
    @Convert(converter = InstantAttributeConverter.class)  // ← Explicit!
    private Instant rowLstUpdtTmstp;

    // ... getters and setters
}
```

---

## How It Works

### When You Save an Entity:

```java
// Your code:
AuthenticationToken token = new AuthenticationToken();
token.setNextExpirTmstp(Instant.now());  // Instant: 2026-01-02T10:30:45Z
tokenRepository.save(token);

// Hibernate processing:
// 1. Sees @Convert(converter = InstantAttributeConverter.class) on nextExpirTmstp field
// 2. Calls: converter.convertToDatabaseColumn(instant)
// 3. Gets: LocalDateTime (2026-01-02T10:30:45) interpreted as UTC
// 4. Sends: LocalDateTime to Oracle JDBC driver
// 5. Oracle stores: TIMESTAMP '2026-01-02 10:30:45'
```

### When You Load an Entity:

```java
// Your code:
Optional<AuthenticationToken> token = tokenRepository.findById(tokenId);

// Hibernate processing:
// 1. Oracle returns: TIMESTAMP '2026-01-02 10:30:45'
// 2. JDBC driver provides: LocalDateTime (2026-01-02T10:30:45)
// 3. Sees @Convert annotation on target field
// 4. Calls: converter.convertToEntityAttribute(localDateTime)
// 5. Gets: Instant (2026-01-02T10:30:45Z) in UTC
// 6. Sets: token.nextExpirTmstp = instant
```

### When You Query with Parameters:

```java
// Your code:
Instant currentUtc = Instant.now();
repository.findByNextExpirTmstpAfter(currentUtc);

// Hibernate processing:
// 1. Generates JPQL with parameter: WHERE t.nextExpirTmstp > :param
// 2. Checks nextExpirTmstp field in entity for @Convert annotation
// 3. Finds: InstantAttributeConverter.class
// 4. Calls: converter.convertToDatabaseColumn(currentUtc)
// 5. Gets: LocalDateTime to bind to query parameter
// 6. Oracle executes: WHERE next_expir_tmstp > '2026-01-02 10:30:45'
```

**Key Point:** Hibernate applies the converter to query parameters automatically because it knows the target field has `@Convert` annotation!

---

## What Gets Converted vs Not Converted

### Database Entity Fields (Converted) ✅

```java
@Entity
public class AuthenticationToken {
    @Convert(converter = InstantAttributeConverter.class)
    private Instant nextExpirTmstp;  // ✅ Converted: Instant → LocalDateTime → Oracle TIMESTAMP
}
```

### DTOs (Not Converted) ✅

```java
public class TokenResponseDTO {
    private Instant expiresAt;  // ✅ Not converted: Just serialized to JSON as ISO-8601
}
```

### Service Layer (Not Converted) ✅

```java
@Service
public class AuthenticationTokenService {
    public String generateToken(String sessionId) {
        Instant expiration = Instant.now().plusSeconds(1800);  // ✅ Not converted: Just Java Instant
        token.setNextExpirTmstp(expiration);  // ✅ Converted when entity is saved
    }
}
```

### Repository Query Parameters (Converted) ✅

```java
// Query parameter uses converter because target entity field has @Convert
Instant currentUtc = Instant.now();
repository.findByNextExpirTmstpAfter(currentUtc);  // ✅ Converted for query parameter binding
```

---

## Adding Converter to New Entities

If you create a new entity with `Instant` fields:

```java
@Entity
@Table(name = "AUDIT_LOG")
public class AuditLog {

    @Id
    private Long id;

    // Must explicitly add @Convert for Oracle compatibility
    @Column(name = "created_at")
    @Convert(converter = InstantAttributeConverter.class)  // ← Add this!
    private Instant createdAt;

    @Column(name = "updated_at")
    @Convert(converter = InstantAttributeConverter.class)  // ← Add this!
    private Instant updatedAt;
}
```

**Without `@Convert`:** Would use Hibernate's default `Instant` → `Timestamp` conversion, which may cause Oracle errors.

**With `@Convert`:** Uses our custom `Instant` → `LocalDateTime` conversion via UTC.

---

## Best Practices

### ✅ DO:

1. **Always annotate database entity `Instant` fields**
   ```java
   @Convert(converter = InstantAttributeConverter.class)
   private Instant timestamp;
   ```

2. **Import the converter explicitly**
   ```java
   import com.wellsfargo.signaturestudio.config.InstantAttributeConverter;
   ```

3. **Document why converter is needed (in entity class javadoc)**
   ```java
   /**
    * Uses InstantAttributeConverter to handle Oracle TIMESTAMP compatibility.
    */
   ```

4. **Test entity persistence with actual Oracle database**
   ```java
   @Test
   void testInstantPersistence() {
       Instant now = Instant.now();
       token.setNextExpirTmstp(now);
       tokenRepository.save(token);

       AuthenticationToken loaded = tokenRepository.findById(token.getId()).get();
       assertEquals(now.truncatedTo(ChronoUnit.MICROS),
                    loaded.getNextExpirTmstp().truncatedTo(ChronoUnit.MICROS));
   }
   ```

### ❌ DON'T:

1. **Don't use `autoApply = true`**
   ```java
   @Converter(autoApply = true)  // ❌ Too broad, affects everything
   ```

2. **Don't forget to add @Convert to new entity fields**
   ```java
   @Column(name = "created_at")
   private Instant createdAt;  // ❌ Missing @Convert annotation!
   ```

3. **Don't apply converter to non-entity classes**
   ```java
   public class UserDTO {
       @Convert(converter = InstantAttributeConverter.class)  // ❌ Not needed for DTOs!
       private Instant loginTime;
   }
   ```

4. **Don't use different time zones**
   ```java
   LocalDateTime.ofInstant(instant, ZoneId.of("America/New_York"))  // ❌ Always use ZoneOffset.UTC!
   ```

---

## Verification Checklist

When adding a new entity with `Instant` fields:

- [ ] Import `InstantAttributeConverter` at top of entity class
- [ ] Add `@Convert(converter = InstantAttributeConverter.class)` to each `Instant` field
- [ ] Verify Oracle database column is `TIMESTAMP` (not `TIMESTAMP WITH TIME ZONE`)
- [ ] Test save/load with actual Oracle database
- [ ] Test queries with `Instant` parameters (e.g., `findBy...After(Instant)`)
- [ ] Verify logs show correct LocalDateTime conversion

---

## Comparison: autoApply vs Manual

| Aspect | autoApply = true | Manual @Convert |
|--------|------------------|-----------------|
| **Scope** | All Instant fields everywhere | Only annotated entity fields |
| **Visibility** | Hidden behavior | Explicit in code |
| **DTOs affected?** | ❌ Yes (unintended) | ✅ No |
| **Discovery** | Must read converter class | Visible on field |
| **Maintenance** | Risky (global changes) | Safe (local changes) |
| **Testing** | Can have side effects | Predictable behavior |
| **Refactoring** | Can break things | Safer |
| **Intent** | Implicit | Explicit |
| **Recommendation** | ❌ Avoid | ✅ Use this |

---

## Summary

**Manual converter application is the better choice because:**

1. ✅ **Explicit** - Clear what's being converted
2. ✅ **Scoped** - Only affects database entities
3. ✅ **Safe** - No surprise conversions
4. ✅ **Maintainable** - Easy to understand and modify
5. ✅ **Testable** - Predictable behavior

**The small overhead of adding `@Convert` annotations is worth it for:**
- Better code clarity
- Safer refactoring
- Fewer surprises for other developers
- More controlled behavior

---

**Document Version:** 1.0
**Last Updated:** 2026-01-02
**Author:** System
**Status:** Production-Ready
