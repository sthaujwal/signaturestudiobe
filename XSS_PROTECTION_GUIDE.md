# XSS Protection Implementation Guide

## Overview

This document describes the XSS (Cross-Site Scripting) protection mechanisms implemented in the Signature Studio application.

## Security Philosophy

**Prevention over Sanitization**: We prefer to **reject** malicious input rather than sanitize it. This approach:
- Provides clearer security boundaries
- Avoids double-encoding issues
- Gives users immediate feedback
- Reduces attack surface

## Implementation Approaches

### ✅ Recommended: Bean Validation with @NoXss (Opt-In)

**Use this approach for most cases.**

#### How It Works
- Custom `@NoXss` annotation applied to DTO fields
- Validates input using regex patterns to detect XSS
- Rejects requests with 400 Bad Request if XSS detected
- Logs security events to audit log

#### Usage Example

```java
public class CreateAccountRequest {

    @NoXss  // ← Add this annotation
    @NotBlank
    @Size(min = 1, max = 255)
    private String accountName;

    @NoXss  // ← Add this annotation
    @NotBlank
    @Size(min = 2, max = 50)
    @Pattern(regexp = "^[A-Z0-9_]+$")
    private String accountKey;

    // Getters and setters
}
```

#### Path Variables and Request Parameters

For `@PathVariable` and `@RequestParam`, add validation at the controller level:

```java
@GetMapping("/accounts/{accountId}")
public ResponseEntity<Account> getAccount(
        @PathVariable @NoXss String accountId) {
    // accountId is now validated
    return ResponseEntity.ok(accountService.getAccount(accountId));
}

@GetMapping("/accounts")
public ResponseEntity<List<Account>> searchAccounts(
        @RequestParam @NoXss String search) {
    // search parameter is validated
    return ResponseEntity.ok(accountService.search(search));
}
```

#### What It Detects

The `@NoXss` validator detects:
- HTML tags: `<script>`, `<iframe>`, `<object>`, `<embed>`, `<applet>`, etc.
- JavaScript event handlers: `onclick=`, `onerror=`, `onload=`, etc.
- JavaScript protocols: `javascript:`, `vbscript:`, `data:text/html`
- IE-specific attacks: `expression()`
- Base64 encoded scripts in image tags

#### Error Response

When XSS is detected, the API returns:
```json
{
  "timestamp": "2025-01-13T10:30:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Input contains potentially unsafe content (HTML tags or scripts are not allowed)"
}
```

### Alternative Approaches (Not Implemented)

If you need different XSS protection strategies:

#### Global Servlet Filter
- Wrap all requests and HTML-encode parameters
- **Pros**: Zero configuration, protects everything
- **Cons**: Encodes ALL input (may break legitimate HTML), double-encoding issues
- **When to use**: Legacy applications without Bean Validation

#### Content Security Policy (CSP)
- Browser-level XSS prevention via HTTP headers
- **Pros**: Defense in depth, blocks execution even if XSS stored
- **Cons**: Doesn't prevent storage of XSS, requires frontend cooperation
- **When to use**: Always (in addition to input validation)

## Where to Apply XSS Protection

### ✅ Apply @NoXss to:
- **User-provided text fields**: names, descriptions, comments
- **Search parameters**: any user input used in queries
- **Path variables**: account IDs, user IDs (if accepting strings)
- **Email addresses**: (though regex already limits characters)
- **URLs**: any user-provided URLs

### ❌ Do NOT apply @NoXss to:
- **Trusted admin content**: If ORG_ADMIN needs to store HTML templates
- **Already validated fields**: Fields with `@Pattern` that only allow safe characters (like `accountKey`)
- **Numeric fields**: integers, longs, etc. (not vulnerable to XSS)
- **Boolean fields**: true/false values
- **Enum fields**: limited to predefined values

## Migration Guide

### Step 1: Add @NoXss to Existing DTOs

Update all request DTOs to include `@NoXss`:

```java
// BEFORE
public class CreateAccountRequest {
    @NotBlank
    private String accountName;
}

// AFTER
public class CreateAccountRequest {
    @NoXss
    @NotBlank
    private String accountName;
}
```

### Step 2: Update Controllers

Add `@NoXss` to path variables and request parameters:

```java
@GetMapping("/accounts/{accountId}")
public ResponseEntity<Account> getAccount(
        @PathVariable @NoXss String accountId,
        @RequestParam(required = false) @NoXss String filter,
        HttpSession session) {
    // ...
}
```

### Step 3: Test

Test with malicious input:
```bash
# Should return 400 Bad Request
curl -X POST http://localhost:8080/api/org-admin/accounts \
  -H "Content-Type: application/json" \
  -d '{"accountName": "<script>alert(1)</script>", "accountKey": "TEST"}'

# Should return 400 Bad Request
curl -X GET "http://localhost:8080/api/accounts?search=<img src=x onerror=alert(1)>"
```

## Output Encoding (Frontend Responsibility)

**Important**: XSS prevention requires both input validation AND output encoding.

### Backend (This Implementation)
- ✅ Validates/rejects malicious input
- ✅ Prevents XSS in stored data

### Frontend (Your Responsibility)
- Must HTML-encode output when rendering user data
- Use React's automatic escaping (JSX)
- Use Angular's built-in sanitization
- Use Vue's v-text (not v-html) for user content

Example (React):
```jsx
// ✅ SAFE - React automatically escapes
<div>{accountName}</div>

// ❌ UNSAFE - Allows XSS
<div dangerouslySetInnerHTML={{__html: accountName}} />
```

## Content Security Policy (Recommended Addition)

Add HTTP headers to prevent XSS execution:

```java
// In SecurityConfig or similar
http.headers()
    .contentSecurityPolicy("default-src 'self'; script-src 'self'; object-src 'none';")
    .and()
    .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
    .and()
    .frameOptions().deny();
```

## Audit Logging

All XSS validation failures are logged to the SECURITY_AUDIT logger:

```
2025-01-13 10:30:00.123 [WARN] SECURITY_AUDIT - XSS_VALIDATION_FAILED | Pattern: <script[^>]*> | Value: <script>alert(1)</script>
```

Monitor these logs for:
- Repeated attempts (possible attack)
- Patterns in attack vectors
- False positives (legitimate input being blocked)

## Testing XSS Protection

### Unit Test Example

```java
@Test
void shouldRejectXssInAccountName() {
    CreateAccountRequest request = new CreateAccountRequest();
    request.setAccountName("<script>alert('xss')</script>");
    request.setAccountKey("TEST");

    Set<ConstraintViolation<CreateAccountRequest>> violations = validator.validate(request);

    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getMessage().contains("unsafe content")));
}
```

### Integration Test Example

```java
@Test
void shouldReturn400WhenXssDetected() throws Exception {
    String maliciousJson = """
        {
            "accountName": "<img src=x onerror=alert(1)>",
            "accountKey": "TEST"
        }
        """;

    mockMvc.perform(post("/api/org-admin/accounts")
            .contentType(MediaType.APPLICATION_JSON)
            .content(maliciousJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(containsString("unsafe content")));
}
```

## Known XSS Vectors (All Handled)

- `<script>alert(1)</script>`
- `<img src=x onerror=alert(1)>`
- `<iframe src="javascript:alert(1)">`
- `<body onload=alert(1)>`
- `<svg/onload=alert(1)>`
- `javascript:alert(1)`
- `<style>@import'http://evil.com/xss.css'</style>`
- `<input onfocus=alert(1) autofocus>`
- `<object data="data:text/html,<script>alert(1)</script>">`

## Performance Considerations

- `@NoXss` validation: ~0.1ms per field (negligible)
- Global filter: ~0.5ms per request (small overhead)
- Regex compilation: Patterns are compiled once at startup

## False Positives

If legitimate input is being rejected:

1. **Review the use case**: Do you really need to accept HTML?
2. **Use allowlist validation**: For specific HTML tags, use a library like OWASP Java HTML Sanitizer
3. **Adjust the pattern**: Modify `NoXssValidator` if needed
4. **Exclude specific fields**: Don't apply `@NoXss` to that field

## Additional Resources

- [OWASP XSS Prevention Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html)
- [OWASP Java HTML Sanitizer](https://github.com/OWASP/java-html-sanitizer)
- [Content Security Policy (CSP)](https://developer.mozilla.org/en-US/docs/Web/HTTP/CSP)

## Summary

**Recommended Approach:**
1. ✅ Use `@NoXss` annotation on all user input fields
2. ✅ Reject malicious input with clear error messages
3. ✅ Log XSS attempts for monitoring
4. ✅ Implement Content Security Policy headers
5. ✅ Ensure frontend properly encodes output

**Avoid:**
- ❌ Global XSS filter (unless you have a specific need)
- ❌ Silent sanitization (makes debugging harder)
- ❌ Over-sanitization (may break legitimate use cases)
