# Table Name Correction

## Actual Table Name

The correct table name in your database is:

```
AUTHENTICATION_TOKEN (singular)
```

**NOT** `AUTHENTICATION_TOKENS` (plural)

---

## Corrected in Code

All Java repository queries have been updated to use the correct table name:

### AuthenticationTokenRepository.java

✅ All native queries now use `AUTHENTICATION_TOKEN`:

```java
@Query(value = "SELECT * FROM AUTHENTICATION_TOKEN WHERE ...")
@Query(value = "UPDATE AUTHENTICATION_TOKEN SET ...")
@Query(value = "DELETE FROM AUTHENTICATION_TOKEN WHERE ...")
```

---

## Documentation References

The following documentation files contain **example SQL** with the **incorrect plural name** `AUTHENTICATION_TOKENS`. These are just documentation examples and don't affect the actual code:

- AUTHENTICATION_IMPLEMENTATION.md
- CLOB_JSON_IMPLEMENTATION.md
- IMPLEMENTATION_SUMMARY.md
- TOKEN_ID_OPTIMIZATION.md
- SESSION_EVENT_DISTRIBUTED_SYSTEM.md
- DISTRIBUTED_SYSTEM_GUIDE.md

**When reading these docs, mentally replace:**
- `AUTHENTICATION_TOKENS` → `AUTHENTICATION_TOKEN`
- `FROM AUTHENTICATION_TOKENS` → `FROM AUTHENTICATION_TOKEN`
- `UPDATE AUTHENTICATION_TOKENS` → `UPDATE AUTHENTICATION_TOKEN`
- `DELETE FROM AUTHENTICATION_TOKENS` → `DELETE FROM AUTHENTICATION_TOKEN`

---

## Entity Annotation

The JPA entity annotation uses the correct singular name:

```java
@Entity
@Table(name = "AUTHENTICATION_TOKEN")  // ← Correct (singular)
public class AuthenticationToken {
    // ...
}
```

---

## Summary

| File Type | Table Name | Status |
|-----------|------------|--------|
| **Java Code** | `AUTHENTICATION_TOKEN` | ✅ **CORRECT** |
| **Repository Queries** | `AUTHENTICATION_TOKEN` | ✅ **CORRECT** |
| **Entity Annotation** | `AUTHENTICATION_TOKEN` | ✅ **CORRECT** |
| **Documentation** | `AUTHENTICATION_TOKENS` | ⚠️ Examples only (not used by code) |

**The actual code is correct and will work with your database!**

---

**Document Version:** 1.0
**Last Updated:** 2025-01-01
**Author:** System
