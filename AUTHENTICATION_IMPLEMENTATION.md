# Authentication Token Implementation Guide

## Overview

This document describes the unified authentication token system implemented for the Signature Studio BFF (Backend For Frontend) application. The system uses custom opaque tokens with automatic session synchronization to provide secure, cross-domain authentication.

## Architecture

### Token Types

The system uses two types of tokens stored in a single unified table:

1. **Authorization Code** (Short-lived)
   - **Lifetime**: 60 seconds
   - **Usage**: One-time use only
   - **Purpose**: Secure token exchange after Ping IdP authentication
   - **Security**: Prevents URL leakage attacks, replay attacks, and browser history exposure

2. **Access Token** (Long-lived)
   - **Lifetime**: 30 minutes (auto-extends on each use)
   - **Usage**: Reusable
   - **Purpose**: API authentication via custom header
   - **Security**: Auto-revocation on logout, session synchronization

### Authentication Flow

```
┌─────────────┐                ┌─────────────┐                ┌─────────────┐
│   Frontend  │                │  BFF/Backend │               │  Ping IdP   │
│ (mywebsite  │                │ (mywebsite   │               │             │
│    .net)    │                │    .com)     │               │             │
└──────┬──────┘                └──────┬───────┘               └──────┬──────┘
       │                              │                              │
       │ 1. Redirect to login         │                              │
       │────────────────────────────> │                              │
       │                              │                              │
       │                              │ 2. Redirect to Ping IdP      │
       │                              │────────────────────────────> │
       │                              │                              │
       │                              │ 3. User authenticates        │
       │                              │                              │
       │                              │ 4. Callback with OAuth code  │
       │                              │ <──────────────────────────┤ │
       │                              │                              │
       │                              │ 5. Exchange code for tokens  │
       │                              │────────────────────────────> │
       │                              │                              │
       │                              │ 6. Receive OAuth tokens      │
       │                              │ <──────────────────────────┤ │
       │                              │                              │
       │                              │ 7. Create session in Oracle  │
       │                              │ 8. Store OAuth tokens        │
       │                              │ 9. Generate auth code (60s)  │
       │                              │                              │
       │ 10. Redirect with auth code  │                              │
       │ <──────────────────────────┤ │                              │
       │ GET /dashboard?code=abc123   │                              │
       │                              │                              │
       │ 11. Extract code from URL    │                              │
       │ 12. Remove code from URL     │                              │
       │     (prevent leakage)        │                              │
       │                              │                              │
       │ 13. Exchange code for token  │                              │
       │ POST /api/auth/exchange      │                              │
       │ { code: "abc123" }           │                              │
       │────────────────────────────> │                              │
       │                              │ 14. Validate code            │
       │                              │ 15. Mark code as used        │
       │                              │ 16. Generate access token    │
       │                              │                              │
       │ 17. Return access token      │                              │
       │ { token: "def456..." }       │                              │
       │ <──────────────────────────┤ │                              │
       │                              │                              │
       │ 18. Store token in memory    │                              │
       │                              │                              │
       │ 19. API requests with token  │                              │
       │ GET /api/dashboard/stats     │                              │
       │ X-SignatureStudio-Token:     │                              │
       │   def456...                  │                              │
       │────────────────────────────> │                              │
       │                              │ 20. Validate token           │
       │                              │ 21. Extend expiration        │
       │                              │ 22. Touch session            │
       │                              │ 23. Return data              │
       │ <──────────────────────────┤ │                              │
```

## Database Schema

```sql
CREATE TABLE AUTHENTICATION_TOKENS (
    authentication_token_id VARCHAR2(64) PRIMARY KEY,
    token_type VARCHAR2(20) NOT NULL,  -- 'AUTHORIZATION_CODE' or 'ACCESS_TOKEN'
    auth_obj VARCHAR2(128) UNIQUE NOT NULL,  -- Token value
    sys_id VARCHAR2(255) NOT NULL,  -- Session ID (FK to SPRING_SESSION)
    expir_prod_in_min NUMBER(10) NOT NULL,  -- Validity in minutes
    next_expir_tmstp TIMESTAMP NOT NULL,  -- Expiration timestamp
    last_used_tmstp TIMESTAMP,  -- Last usage (ACCESS_TOKEN)
    used_at TIMESTAMP,  -- Consumption time (AUTHORIZATION_CODE)
    row_crte_tmstp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    row_lst_updt_tmstp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT chk_token_type CHECK (token_type IN ('AUTHORIZATION_CODE', 'ACCESS_TOKEN')),
    CONSTRAINT fk_auth_token_session FOREIGN KEY (sys_id)
        REFERENCES SPRING_SESSION(SESSION_ID) ON DELETE CASCADE
);
```

## Implementation Components

### 1. Entity: AuthenticationToken.java

Located: `src/main/java/com/wellsfargo/signaturestudio/domain/AuthenticationToken.java`

**Key Methods:**
- `isExpired()` - Check if token has expired
- `isUsed()` - Check if authorization code was already consumed
- `markAsUsed()` - Mark authorization code as consumed (one-time use)
- `extendExpiration()` - Extend access token expiration (auto-refresh)

### 2. Repository: AuthenticationTokenRepository.java

Located: `src/main/java/com/wellsfargo/signaturestudio/repository/AuthenticationTokenRepository.java`

**Key Methods:**
- `findByAuthObj(String authObj)` - Find token by value
- `deleteBySysId(String sysId)` - Delete all tokens for session (logout)
- `deleteExpiredTokens(Instant now)` - Cleanup expired tokens
- `deleteOldUsedAuthorizationCodes(Instant threshold)` - Cleanup old codes

### 3. Service: AuthenticationTokenService.java

Located: `src/main/java/com/wellsfargo/signaturestudio/service/AuthenticationTokenService.java`

**Key Methods:**
- `generateAuthorizationCode(String sessionId)` - Generate 60-second code
- `generateAccessToken(String sessionId)` - Generate 30-minute token
- `validateAndConsumeAuthorizationCode(String code)` - One-time validation
- `validateAndExtendAccessToken(String token)` - Validate and auto-extend
- `revokeTokensForSession(String sessionId)` - Revoke all tokens on logout
- `cleanupExpiredTokens()` - Scheduled cleanup (every 5 minutes)

### 4. Filter: TokenAuthenticationFilter.java

Located: `src/main/java/com/wellsfargo/signaturestudio/config/TokenAuthenticationFilter.java`

**Purpose:** Intercept requests, validate tokens, extend expiration, and synchronize with session

**Excluded Paths:**
- `/api/public/**`
- `/api/auth/login`
- `/api/auth/callback`
- `/api/auth/exchange`

### 5. Event Listener: SessionEventListener.java

Located: `src/main/java/com/wellsfargo/signaturestudio/config/SessionEventListener.java`

**Purpose:** Automatically revoke tokens when sessions are destroyed (logout, timeout)

**Events Handled:**
- `SessionDeletedEvent` - Explicit logout
- `SessionExpiredEvent` - Session timeout

### 6. Controller: AuthController.java

Located: `src/main/java/com/wellsfargo/signaturestudio/controller/AuthController.java`

**New Endpoint:**
```java
POST /api/auth/exchange
Request: { "code": "authorization_code" }
Response: { "token": "access_token", "error": null }
```

### 7. Security Config: SecurityConfig.java

Located: `src/main/java/com/wellsfargo/signaturestudio/config/SecurityConfig.java`

**Changes:**
- Disabled CSRF (token in custom header provides CSRF protection)
- Added `TokenAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`
- Changed session policy to `STATELESS` (no cookie-based sessions)
- Permitted `/api/auth/exchange` without authentication

## Security Features

### 1. Authorization Code Protection

✅ **One-time use** - Code is marked as consumed after first use
✅ **Short-lived** - Expires in 60 seconds
✅ **URL leakage protection** - Code in URL is useless after exchange
✅ **Browser history protection** - Logged codes are already consumed
✅ **Replay attack prevention** - `used_at` timestamp prevents reuse

### 2. Access Token Protection

✅ **Custom header** - Token in `X-SignatureStudio-Token` (not cookie)
✅ **Cross-domain support** - Works without third-party cookies
✅ **Auto-extension** - Extends on every valid request (sliding expiration)
✅ **Session synchronization** - Token and session expire together
✅ **Immediate revocation** - Logout deletes tokens from database

### 3. CSRF Protection

✅ **No CSRF token needed** - Custom header cannot be forged by attackers
✅ **Same-Origin Policy** - Browser prevents malicious sites from reading tokens
✅ **No automatic sending** - Token must be explicitly included (unlike cookies)

### 4. Session Fixation Protection

✅ **New session on login** - Session created after Ping IdP authentication
✅ **Session ID rotation** - Each login creates new session ID
✅ **Token tied to session** - Tokens reference specific session ID

## Frontend Integration

### 1. Handle Redirect with Authorization Code

```javascript
// Extract authorization code from URL
const urlParams = new URLSearchParams(window.location.search);
const authCode = urlParams.get('code');

if (!authCode) {
    console.error('No authorization code in URL');
    window.location.href = '/login';
    return;
}

// CRITICAL: Remove code from URL immediately (prevent leakage)
window.history.replaceState({}, '', '/dashboard');
```

### 2. Exchange Code for Access Token

```javascript
try {
    // Exchange short-lived code for long-lived token
    const response = await fetch('https://backend.com/api/auth/exchange', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ code: authCode })
    });

    if (!response.ok) {
        throw new Error('Token exchange failed');
    }

    const { token, error } = await response.json();

    if (error) {
        throw new Error(error);
    }

    // Store long-lived token
    sessionStorage.setItem('authToken', token);

    // Load dashboard data
    loadDashboard();

} catch (error) {
    console.error('Authentication failed:', error);
    sessionStorage.removeItem('authToken');
    window.location.href = '/login';
}
```

### 3. Use Access Token in API Calls

```javascript
// Create API client
const apiClient = {
    async request(url, options = {}) {
        const token = sessionStorage.getItem('authToken');

        if (!token) {
            window.location.href = '/login';
            return;
        }

        const response = await fetch(url, {
            ...options,
            headers: {
                ...options.headers,
                'X-SignatureStudio-Token': token  // Custom header
            }
        });

        if (response.status === 401) {
            sessionStorage.removeItem('authToken');
            window.location.href = '/login';
            return;
        }

        return response;
    }
};

// Usage
async function loadDashboard() {
    const response = await apiClient.request('https://backend.com/api/dashboard/stats');
    const data = await response.json();
    // Render dashboard
}
```

## Configuration

### Application Properties

No additional configuration needed. The system uses existing Spring Session configuration:

```properties
# Spring Session Configuration (Oracle)
spring.session.store-type=jdbc
spring.session.jdbc.initialize-schema=always
spring.session.jdbc.table-name=SPRING_SESSION
spring.session.timeout=30m
```

### Token Configuration

Token parameters are defined in `AuthenticationTokenService.java`:

```java
// Authorization Code Configuration
private static final int AUTHORIZATION_CODE_LENGTH = 32;  // bytes (43 chars Base64)
private static final int AUTHORIZATION_CODE_VALIDITY_MIN = 1;  // 1 minute

// Access Token Configuration
private static final int ACCESS_TOKEN_LENGTH = 48;  // bytes (64 chars Base64)
private static final int ACCESS_TOKEN_VALIDITY_MIN = 30;  // 30 minutes
```

To change token validity, modify these constants and rebuild.

## Deployment Steps

### 1. Run Database Migration

```sql
-- Execute migration script
@src/main/resources/db/migration/V001__Create_Authentication_Tokens_Table.sql
```

### 2. Verify Table Creation

```sql
-- Check table exists
SELECT * FROM USER_TABLES WHERE TABLE_NAME = 'AUTHENTICATION_TOKENS';

-- Check indexes
SELECT * FROM USER_INDEXES WHERE TABLE_NAME = 'AUTHENTICATION_TOKENS';

-- Check trigger
SELECT * FROM USER_TRIGGERS WHERE TRIGGER_NAME = 'TRG_AUTH_TOKEN_UPDATE';
```

### 3. Build Application

```bash
./gradlew clean build
```

### 4. Deploy to Environment

Deploy the updated JAR to your environment (dev, staging, prod).

### 5. Configure Ping IdP Callback

Update Ping IdP configuration to redirect to:
```
https://backend.com/api/auth/callback
```

After authentication, backend will redirect to:
```
https://frontend.net/dashboard?code={authorization_code}
```

## Monitoring and Maintenance

### Scheduled Tasks

**Token Cleanup** (runs every 5 minutes):
- Deletes expired tokens
- Deletes used authorization codes older than 5 minutes
- Logs cleanup metrics

### Logging

**Important Log Messages:**

```
# Token generation
INFO: Generated AUTHORIZATION_CODE for session: {sessionId} (expires in 1 minutes)
INFO: Generated ACCESS_TOKEN for session: {sessionId} (expires in 30 minutes)

# Token validation
DEBUG: Access token validated and extended for session: {sessionId} (new expiry: {timestamp})
WARN: Authorization code already used for session: {sessionId}
WARN: Access token expired for session: {sessionId}

# Token revocation
INFO: Session deleted, revoking all tokens: {sessionId}
INFO: Session expired, revoking all tokens: {sessionId}
INFO: Revoked {count} token(s) for session: {sessionId}

# Cleanup
INFO: Cleanup: {expiredCount} expired tokens, {usedCodesCount} old authorization codes
```

### Database Queries

**Check active tokens:**
```sql
SELECT token_type, COUNT(*) as count,
       MIN(next_expir_tmstp) as earliest_expiry,
       MAX(next_expir_tmstp) as latest_expiry
FROM AUTHENTICATION_TOKENS
WHERE next_expir_tmstp > CURRENT_TIMESTAMP
GROUP BY token_type;
```

**Find expired tokens:**
```sql
SELECT * FROM AUTHENTICATION_TOKENS
WHERE next_expir_tmstp < CURRENT_TIMESTAMP;
```

**Check used authorization codes:**
```sql
SELECT * FROM AUTHENTICATION_TOKENS
WHERE token_type = 'AUTHORIZATION_CODE'
AND used_at IS NOT NULL
ORDER BY used_at DESC;
```

**Token usage statistics:**
```sql
SELECT
    token_type,
    COUNT(*) as total_tokens,
    COUNT(CASE WHEN next_expir_tmstp > CURRENT_TIMESTAMP THEN 1 END) as active_tokens,
    COUNT(CASE WHEN next_expir_tmstp < CURRENT_TIMESTAMP THEN 1 END) as expired_tokens,
    COUNT(CASE WHEN used_at IS NOT NULL THEN 1 END) as used_codes
FROM AUTHENTICATION_TOKENS
GROUP BY token_type;
```

## Troubleshooting

### Issue: "Invalid or expired authorization code"

**Possible Causes:**
1. Code already used (replay attack attempt)
2. Code expired (>60 seconds since generation)
3. Code not found in database

**Solution:**
- Check logs for "Authorization code already used" or "Authorization code expired"
- Verify frontend removes code from URL immediately after extraction
- Ensure network latency doesn't cause timeout (60 seconds is generous)

### Issue: "Invalid or expired token" (401)

**Possible Causes:**
1. Access token expired (no activity for >30 minutes)
2. Session destroyed (logout or timeout)
3. Token not found in database

**Solution:**
- Redirect user to login page
- Check if session was destroyed prematurely
- Verify scheduled cleanup isn't too aggressive

### Issue: Tokens not being cleaned up

**Possible Causes:**
1. Scheduled task not running
2. Database permissions issue

**Solution:**
- Check application logs for cleanup messages
- Verify `@EnableScheduling` is enabled
- Manually run cleanup query if needed

## Future Enhancements

### Potential Improvements

1. **Refresh Tokens**
   - Add third token type: `REFRESH_TOKEN`
   - Allow token refresh without re-authentication
   - Longer lifetime (7 days) for refresh tokens

2. **Token Rotation**
   - Rotate access token on each refresh
   - Invalidate old token after rotation
   - Detect token theft via rotation chain

3. **Device Tracking**
   - Store user agent and IP address with tokens
   - Detect suspicious activity (IP changes, etc.)
   - Alert user of new device logins

4. **Rate Limiting**
   - Limit token exchange attempts per session
   - Prevent brute force attacks on authorization codes
   - Lock out after failed attempts

5. **Audit Trail**
   - Detailed logging of token lifecycle
   - Track token usage patterns
   - Generate security reports

## Support

For questions or issues, contact the development team or file an issue in the project repository.

---

**Document Version:** 1.0
**Last Updated:** 2025-01-01
**Author:** System
