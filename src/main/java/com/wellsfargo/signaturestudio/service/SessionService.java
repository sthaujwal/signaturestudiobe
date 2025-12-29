package com.wellsfargo.signaturestudio.service;

import com.wellsfargo.signaturestudio.constants.SessionConstants;
import com.wellsfargo.signaturestudio.domain.AccountWithRole;
import com.wellsfargo.signaturestudio.domain.Session;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing Spring Session operations.
 * 
 * Responsibilities:
 * - Retrieving session information
 * - Validating session state
 * - Getting user information from session
 * - Updating session metadata (last access time)
 * 
 * This service abstracts session access, making it easier to:
 * - Test services without HttpSession
 * - Change session implementation
 * - Add session-related business logic
 */
@Service
public class SessionService {
    
    private static final Logger auditLogger = LoggerFactory.getLogger("SECURITY_AUDIT");
    
    /**
     * Gets current session information as DTO.
     * 
     * @param session The HTTP session
     * @return Session with session information, or null if session is invalid
     */
    public Session getSessionInfo(HttpSession session) {
        if (session == null) {
            return null;
        }
        
        String username = getUsername(session);
        if (username == null) {
            return null;
        }
        
        Boolean authenticated = (Boolean) session.getAttribute(SessionConstants.AUTHENTICATED);
        if (authenticated == null || !authenticated) {
            return null;
        }
        
        // Update last access time
        updateLastAccessTime(session);
        
        Session sessionDTO = new Session();
        sessionDTO.setSessionId(session.getId());
        sessionDTO.setUsername(username);
        sessionDTO.setEmail(getEmail(session));
        sessionDTO.setAccountId(getAccountId(session));
        
        // Include accounts with roles from session
        List<AccountWithRole> accountsWithRoles = getAccountsWithRoles(session);
        sessionDTO.setAccountsWithRoles(accountsWithRoles);
        
        // Calculate times
        Long loginTime = (Long) session.getAttribute(SessionConstants.LOGIN_TIME);
        if (loginTime != null) {
            Instant createdAt = Instant.ofEpochMilli(loginTime);
            sessionDTO.setCreatedAt(createdAt);
            sessionDTO.setExpiresAt(createdAt.plusSeconds(SessionConstants.SESSION_TIMEOUT_SECONDS));
        }
        
        return sessionDTO;
    }
    
    /**
     * Gets current session information from HttpServletRequest.
     * 
     * @param request The HTTP request
     * @return Session with session information, or null if session is invalid
     */
    public Session getSessionInfo(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return getSessionInfo(session);
    }
    
    /**
     * Validates if session is still valid and not tampered.
     * 
     * @param request The HTTP request
     * @return true if session is valid, false otherwise
     */
    public boolean isSessionValid(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        
        // Check authenticated flag
        Boolean authenticated = (Boolean) session.getAttribute(SessionConstants.AUTHENTICATED);
        if (authenticated == null || !authenticated) {
            return false;
        }
        
        // Check username exists
        String username = getUsername(session);
        if (username == null || username.isEmpty()) {
            return false;
        }
        
        // Optional: Check if client IP changed (session hijacking detection)
        // This can be disabled if users are behind dynamic proxies
        String storedIp = (String) session.getAttribute(SessionConstants.CLIENT_IP);
        String currentIp = getClientIp(request);
        if (storedIp != null && !storedIp.equals(currentIp)) {
            auditLogger.warn("SECURITY_EVENT | Type: IP_MISMATCH | User: {} | StoredIP: {} | CurrentIP: {} | SessionId: {}",
                username, storedIp, currentIp, session.getId());
            // Uncomment to enforce IP binding (may cause issues with mobile users):
            // return false;
        }
        
        return true;
    }
    
    /**
     * Gets username from session.
     * 
     * @param session The HTTP session
     * @return Username or null if not found
     */
    public String getUsername(HttpSession session) {
        if (session == null) {
            return null;
        }
        return (String) session.getAttribute(SessionConstants.USERNAME);
    }
    
    /**
     * Gets username from request.
     * 
     * @param request The HTTP request
     * @return Username or null if not found
     */
    public String getUsername(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return getUsername(session);
    }
    
    /**
     * Gets email from session.
     * 
     * @param session The HTTP session
     * @return Email or null if not found
     */
    public String getEmail(HttpSession session) {
        if (session == null) {
            return null;
        }
        return (String) session.getAttribute(SessionConstants.EMAIL);
    }
    
    /**
     * Gets account ID from session.
     * 
     * @param session The HTTP session
     * @return Account ID or null if not found
     */
    public String getAccountId(HttpSession session) {
        if (session == null) {
            return null;
        }
        return (String) session.getAttribute(SessionConstants.ACCOUNT_ID);
    }
    
    /**
     * Gets account ID from request.
     * 
     * @param request The HTTP request
     * @return Account ID or null if not found
     */
    public String getAccountId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return getAccountId(session);
    }
    
    /**
     * Checks if session is authenticated.
     * 
     * @param session The HTTP session
     * @return true if authenticated, false otherwise
     */
    public boolean isAuthenticated(HttpSession session) {
        if (session == null) {
            return false;
        }
        Boolean authenticated = (Boolean) session.getAttribute(SessionConstants.AUTHENTICATED);
        return authenticated != null && authenticated;
    }
    
    /**
     * Updates the last access time in session.
     * 
     * @param session The HTTP session
     */
    public void updateLastAccessTime(HttpSession session) {
        if (session != null) {
            session.setAttribute(SessionConstants.LAST_ACCESS_TIME, System.currentTimeMillis());
        }
    }
    
    /**
     * Gets client IP address from request, handling proxies.
     * 
     * @param request The HTTP request
     * @return Client IP address
     */
    public String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
    
    /**
     * Gets session ID from request.
     * 
     * @param request The HTTP request
     * @return Session ID or null if no session
     */
    public String getSessionId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null ? session.getId() : null;
    }
    
    /**
     * Switches the current account in the session.
     * Updates the accountId in session and logs the switch for audit purposes.
     * 
     * @param session The HTTP session
     * @param newAccountId The new account ID to switch to
     * @param username The username (for audit logging)
     * @return The previous account ID, or null if no previous account was set
     */
    public String switchAccount(HttpSession session, String newAccountId, String username) {
        if (session == null) {
            throw new IllegalArgumentException("Session cannot be null");
        }
        
        String previousAccountId = getAccountId(session);
        
        // Update account ID in session
        session.setAttribute(SessionConstants.ACCOUNT_ID, newAccountId);
        
        // Audit log the account switch
        auditLogger.info("ACCOUNT_SWITCH | User: {} | PreviousAccount: {} | NewAccount: {} | SessionId: {}",
            username, previousAccountId, newAccountId, session.getId());
        
        return previousAccountId;
    }
    
    /**
     * Switches the current account in the session from HttpServletRequest.
     * 
     * @param request The HTTP request
     * @param newAccountId The new account ID to switch to
     * @param username The username (for audit logging)
     * @return The previous account ID, or null if no previous account was set
     */
    public String switchAccount(HttpServletRequest request, String newAccountId, String username) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new IllegalStateException("No active session found");
        }
        return switchAccount(session, newAccountId, username);
    }
    
    /**
     * Sets account ID in session (used during login or account switch).
     * 
     * @param session The HTTP session
     * @param accountId The account ID to set
     */
    public void setAccountId(HttpSession session, String accountId) {
        if (session != null) {
            session.setAttribute(SessionConstants.ACCOUNT_ID, accountId);
        }
    }
    
    // ============================================
    // CONVENIENCE METHODS - Use RequestContextHolder
    // These methods automatically get the current request/session
    // Use when you don't have HttpSession/HttpServletRequest available
    // ============================================
    
    /**
     * Gets the current session from RequestContextHolder.
     * Convenience method when HttpSession is not available as a parameter.
     * 
     * @return Current HttpSession or null if not available
     */
    private HttpSession getCurrentSession() {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return attributes.getRequest().getSession(false);
    }
    
    /**
     * Gets the current request from RequestContextHolder.
     * Convenience method when HttpServletRequest is not available as a parameter.
     * 
     * @return Current HttpServletRequest or null if not available
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return attributes.getRequest();
    }
    
    /**
     * Gets current session information using RequestContextHolder.
     * Convenience method - automatically gets session from current request.
     * 
     * @return Session with session information, or null if session is invalid
     * @throws IllegalStateException if called outside of a request context
     */
    public Session getCurrentSessionInfo() {
        HttpSession session = getCurrentSession();
        if (session == null) {
            throw new IllegalStateException("No active session found. This method must be called within a request context.");
        }
        return getSessionInfo(session);
    }
    
    /**
     * Gets username from current session using RequestContextHolder.
     * Convenience method - automatically gets session from current request.
     * 
     * @return Username or null if not found
     * @throws IllegalStateException if called outside of a request context
     */
    public String getCurrentUsername() {
        HttpSession session = getCurrentSession();
        if (session == null) {
            return null;
        }
        return getUsername(session);
    }
    
    /**
     * Gets account ID from current session using RequestContextHolder.
     * Convenience method - automatically gets session from current request.
     * 
     * @return Account ID or null if not found
     * @throws IllegalStateException if called outside of a request context
     */
    public String getCurrentAccountId() {
        HttpSession session = getCurrentSession();
        if (session == null) {
            return null;
        }
        return getAccountId(session);
    }
    
    /**
     * Gets email from current session using RequestContextHolder.
     * Convenience method - automatically gets session from current request.
     * 
     * @return Email or null if not found
     */
    public String getCurrentEmail() {
        HttpSession session = getCurrentSession();
        if (session == null) {
            return null;
        }
        return getEmail(session);
    }
    
    /**
     * Validates if current session is valid using RequestContextHolder.
     * Convenience method - automatically gets request from current context.
     * 
     * @return true if session is valid, false otherwise
     */
    public boolean isCurrentSessionValid() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return false;
        }
        return isSessionValid(request);
    }
    
    /**
     * Switches account in current session using RequestContextHolder.
     * Convenience method - automatically gets session from current request.
     * 
     * @param newAccountId The new account ID to switch to
     * @return The previous account ID, or null if no previous account was set
     * @throws IllegalStateException if called outside of a request context or no session exists
     */
    public String switchCurrentAccount(String newAccountId) {
        HttpSession session = getCurrentSession();
        if (session == null) {
            throw new IllegalStateException("No active session found. This method must be called within a request context.");
        }
        String username = getUsername(session);
        if (username == null) {
            throw new IllegalStateException("No username found in session");
        }
        return switchAccount(session, newAccountId, username);
    }
    
    /**
     * Gets user's accounts with roles from session.
     * 
     * @param session The HTTP session
     * @return List of accounts with roles, or empty list if not found
     */
    public List<AccountWithRole> getAccountsWithRoles(HttpSession session) {
        if (session == null) {
            return new ArrayList<>();
        }
        @SuppressWarnings("unchecked")
        List<AccountWithRole> accounts = (List<AccountWithRole>) 
            session.getAttribute(SessionConstants.ACCOUNTS_WITH_ROLES);
        return accounts != null ? accounts : new ArrayList<>();
    }
    
    /**
     * Gets user's accounts with roles from request.
     * 
     * @param request The HTTP request
     * @return List of accounts with roles, or empty list if not found
     */
    public List<AccountWithRole> getAccountsWithRoles(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return getAccountsWithRoles(session);
    }
    
    /**
     * Gets current user's accounts with roles using RequestContextHolder.
     * 
     * @return List of accounts with roles, or empty list if not found
     */
    public List<AccountWithRole> getCurrentAccountsWithRoles() {
        HttpSession session = getCurrentSession();
        return getAccountsWithRoles(session);
    }
}

