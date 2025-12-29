package com.wellsfargo.signaturestudio.constants;

/**
 * Centralized constants for Spring Session attribute keys.
 * 
 * This class provides a single source of truth for all session attribute names,
 * preventing magic strings and making refactoring easier.
 * 
 * Usage:
 * <pre>
 * session.setAttribute(SessionConstants.USERNAME, username);
 * String username = (String) session.getAttribute(SessionConstants.USERNAME);
 * </pre>
 */
public final class SessionConstants {
    
    private SessionConstants() {
        // Utility class - prevent instantiation
    }
    
    // Authentication & User Information
    /** Session key for authenticated username */
    public static final String USERNAME = "username";
    
    /** Session key for user email address */
    public static final String EMAIL = "email";
    
    /** Session key for user account ID */
    public static final String ACCOUNT_ID = "accountId";
    
    /** Session key for user's accounts with roles (List<AccountWithRole>) */
    public static final String ACCOUNTS_WITH_ROLES = "accountsWithRoles";
    
    /** Session key for authentication status (Boolean) */
    public static final String AUTHENTICATED = "authenticated";
    
    // Session Metadata
    /** Session key for login timestamp (Long - milliseconds since epoch) */
    public static final String LOGIN_TIME = "loginTime";
    
    /** Session key for last access timestamp (Long - milliseconds since epoch) */
    public static final String LAST_ACCESS_TIME = "lastAccessTime";
    
    // Security Information
    /** Session key for client IP address at login */
    public static final String CLIENT_IP = "clientIp";
    
    /** Session key for user agent string at login */
    public static final String USER_AGENT = "userAgent";
    
    // Session Configuration
    /** Default session timeout in seconds (30 minutes) */
    public static final int SESSION_TIMEOUT_SECONDS = 30 * 60;
    
    /** Session timeout in milliseconds */
    public static final long SESSION_TIMEOUT_MILLIS = SESSION_TIMEOUT_SECONDS * 1000L;
}

