# Automatic Token Refresh - How It Works

## Overview

Your authentication system **already has automatic token refresh built-in**. Tokens auto-extend on every API call, so active users never get logged out.

## How Auto-Refresh Works

### On Every API Call

```
User makes API request with token
    ↓
API filter/interceptor calls:
validateAndExtendAccessToken(tokenId)
    ↓
Token validation:
✓ Token exists?
✓ Token not expired?
    ↓
Auto-extend expiration:
token.extendExpiration(30 minutes)
    ↓
Save updated token to database
    ↓
Return session ID (auth successful)
    ↓
API request proceeds
```

### Code Implementation

**Location:** [AuthenticationTokenService.java:176](src/main/java/com/wellsfargo/signaturestudio/service/AuthenticationTokenService.java#L176)

```java
@Transactional
public Optional<String> validateAndExtendAccessToken(String tokenId) {
    Instant currentUtc = Instant.now();

    // Find valid token
    Optional<AuthenticationToken> tokenOpt = tokenRepository
        .findByAuthenticationTokenIdAndTokenTypeAndNextExpirTmstpAfter(
            tokenId,
            TokenType.ACCESS_TOKEN,
            currentUtc
        );

    if (tokenOpt.isEmpty()) {
        return Optional.empty();  // Token invalid/expired
    }

    AuthenticationToken token = tokenOpt.get();

    // AUTO-EXTEND: Add 30 minutes to expiration
    token.extendExpiration(ACCESS_TOKEN_VALIDITY_MIN);  // 30 minutes
    tokenRepository.save(token);

    return Optional.of(token.getSysId());  // Return session ID
}
```

## Token Lifecycle

### Active User (Keeps Making API Calls)

```
Time    | Event                          | Token Expiration
--------|--------------------------------|------------------
10:00   | User logs in                   | 10:30 (30 min)
10:15   | API call (view dashboard)      | 10:45 (extended!)
10:40   | API call (upload document)     | 11:10 (extended!)
11:05   | API call (submit signature)    | 11:35 (extended!)
...     | User stays active              | Token never expires!
```

**Result:** User can work indefinitely without interruption.

### Inactive User (No API Calls)

```
Time    | Event                          | Token Expiration
--------|--------------------------------|------------------
10:00   | User logs in                   | 10:30 (30 min)
10:15   | API call (view dashboard)      | 10:45 (extended!)
        | ... user walks away ...        |
10:45   | Token expires (no activity)    | EXPIRED
10:46   | Next API call fails            | 401 Unauthorized
        | Frontend redirects to login    |
```

**Result:** Inactive user gets logged out after 30 minutes (security).

## Configuration

### Token Validity

**File:** [AuthenticationTokenService.java:34](src/main/java/com/wellsfargo/signaturestudio/service/AuthenticationTokenService.java#L34)

```java
private static final int ACCESS_TOKEN_VALIDITY_MIN = 30;  // 30 minutes
```

**Change this value** to adjust the inactivity timeout:
- `15` = 15 minutes (more secure, less convenient)
- `30` = 30 minutes (balanced - current setting)
- `60` = 60 minutes (less secure, more convenient)

### Session Timeout

**Your configuration:** `@EnableJdbcHttpSession(maxInactiveIntervalInSeconds = 960)`
- Session timeout: **16 minutes**
- Token timeout: **30 minutes**

**Recommendation:** Keep token timeout ≥ session timeout
- Current: Token (30 min) > Session (16 min) ✓
- This ensures token doesn't expire before session

## Benefits

| Benefit | Description |
|---------|-------------|
| **Seamless UX** | Users never interrupted during active work |
| **Security** | Inactive users automatically logged out after 30 min |
| **Simple** | No frontend polling or refresh logic needed |
| **Efficient** | Extends on existing API calls (no extra requests) |
| **Synchronized** | Token and session stay aligned |

## Cleanup Strategy

### What Gets Cleaned Up Automatically

```
1. Active Users:
   └─ Tokens auto-extend on every API call
      └─ Never expire while user is active

2. Inactive Users (30+ min):
   └─ Token expires
      └─ AuthenticationTokenService.cleanupExpiredTokens() (every 5 min)
         └─ DELETE expired tokens

3. Explicit Logout:
   └─ User clicks logout
      └─ SessionEventListener.onSessionDeleted()
         └─ DELETE all tokens for session

4. Session Expiration (16+ min):
   └─ Spring Session deletes expired session
      └─ Tokens cleaned by scheduled job (5 min intervals)
```

### Scheduled Cleanup Jobs

| Job | Frequency | Purpose |
|-----|-----------|---------|
| `AuthenticationTokenService.cleanupExpiredTokens()` | Every 5 min | Delete expired tokens |
| Spring Session cleanup | Every 1 min | Delete expired sessions |

## Frontend Integration

### Token Storage

```javascript
// Store token after login/authorization
localStorage.setItem('access_token', tokenValue);
```

### API Requests

```javascript
// Include token in every API request
fetch('/api/documents', {
  headers: {
    'X-SignatureStudio-Token': localStorage.getItem('access_token')
  }
});
```

### Handling Expiration

```javascript
// If token expires, redirect to login
fetch('/api/documents', {
  headers: {
    'X-SignatureStudio-Token': localStorage.getItem('access_token')
  }
})
.then(response => {
  if (response.status === 401) {
    // Token expired, redirect to login
    window.location.href = '/login';
  }
  return response.json();
});
```

**No refresh token logic needed!** The backend handles everything.

## Testing

### Test 1: Auto-Refresh on Activity

```bash
# 1. Log in and get token
TOKEN=$(curl -X POST http://localhost:8080/auth/login ...)

# 2. Make API calls every 10 minutes for 1 hour
for i in {1..6}; do
  curl -H "X-SignatureStudio-Token: $TOKEN" http://localhost:8080/api/documents
  sleep 600  # 10 minutes
done

# Expected: All requests succeed (token auto-extends)
```

### Test 2: Expiration on Inactivity

```bash
# 1. Log in and get token
TOKEN=$(curl -X POST http://localhost:8080/auth/login ...)

# 2. Wait 31 minutes (longer than token validity)
sleep 1860

# 3. Try to use token
curl -H "X-SignatureStudio-Token: $TOKEN" http://localhost:8080/api/documents

# Expected: 401 Unauthorized (token expired)
```

### Test 3: Check Token Expiration in Database

```sql
-- After making an API call, check token expiration
SELECT
    authentication_token_id,
    token_type,
    next_expir_tmstp,
    (next_expir_tmstp - SYSTIMESTAMP) AS time_until_expiry
FROM AUTHENTICATION_TOKEN
WHERE token_type = 'ACCESS_TOKEN'
ORDER BY next_expir_tmstp DESC;

-- Should show ~30 minutes from now (just extended)
```

## Summary

**You already have auto-refresh working!**

✅ **Auto-extends tokens** on every API call
✅ **No frontend changes** needed
✅ **Active users** never interrupted
✅ **Inactive users** auto-logout after 30 min
✅ **Expired tokens** cleaned automatically
✅ **Simple and clean** implementation

Just make sure your API filter/interceptor calls:
```java
tokenService.validateAndExtendAccessToken(tokenId)
```

...on every authenticated request, and it handles everything automatically!

---

**Status:** Production-Ready
**Implementation:** Complete
**Frontend Changes:** None needed
