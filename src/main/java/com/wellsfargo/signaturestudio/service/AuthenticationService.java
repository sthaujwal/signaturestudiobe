package com.wellsfargo.signaturestudio.service;

import com.wellsfargo.signaturestudio.dto.LoginRequestDTO;
import com.wellsfargo.signaturestudio.dto.SessionDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Authentication service with enterprise security best practices.
 * 
 * Security features:
 * 1. Session fixation protection (new session on login)
 * 2. Secure session attributes
 * 3. Audit logging for security events
 * 4. Proper session invalidation on logout
 */
@Service
public class AuthenticationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("SECURITY_AUDIT");
    
    // Session attribute keys
    public static final String SESSION_USER_KEY = "username";
    public static final String SESSION_EMAIL_KEY = "email";
    public static final String SESSION_ACCOUNT_KEY = "accountId";
    public static final String SESSION_AUTHENTICATED_KEY = "authenticated";
    public static final String SESSION_LOGIN_TIME_KEY = "loginTime";
    public static final String SESSION_LAST_ACCESS_KEY = "lastAccessTime";
    public static final String SESSION_CLIENT_IP_KEY = "clientIp";
    public static final String SESSION_USER_AGENT_KEY = "userAgent";
    
    // Session timeout in seconds (30 minutes)
    private static final int SESSION_TIMEOUT_SECONDS = 30 * 60;
    
    /**
     * Authenticates user and creates secure session.
     * Implements session fixation protection by creating new session.
     */
    public SessionDTO login(LoginRequestDTO loginRequest, HttpServletRequest request) {
        String username = loginRequest.getUsername();
        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        
        logger.info("Login attempt for user: {} from IP: {}", username, clientIp);
        
        // SESSION FIXATION PROTECTION: Invalidate existing session and create new one
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            String oldSessionId = oldSession.getId();
            oldSession.invalidate();
            logger.debug("Invalidated old session {} for session fixation protection", oldSessionId);
        }
        
        // Create new session
        HttpSession session = request.getSession(true);
        String newSessionId = session.getId();
        
        // Mock authentication - will be replaced with real Ping integration
        // In production, validate credentials here
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusSeconds(SESSION_TIMEOUT_SECONDS);
        
        // Store secure session attributes
        session.setAttribute(SESSION_USER_KEY, username);
        session.setAttribute(SESSION_EMAIL_KEY, username + "@wellsfargo.com");
        session.setAttribute(SESSION_ACCOUNT_KEY, "ACCT_" + username.hashCode());
        session.setAttribute(SESSION_AUTHENTICATED_KEY, true);
        session.setAttribute(SESSION_LOGIN_TIME_KEY, System.currentTimeMillis());
        session.setAttribute(SESSION_LAST_ACCESS_KEY, System.currentTimeMillis());
        session.setAttribute(SESSION_CLIENT_IP_KEY, clientIp);
        session.setAttribute(SESSION_USER_AGENT_KEY, userAgent);
        session.setMaxInactiveInterval(SESSION_TIMEOUT_SECONDS);
        
        // Build response DTO
        SessionDTO sessionDTO = new SessionDTO();
        sessionDTO.setSessionId(newSessionId);
        sessionDTO.setUsername(username);
        sessionDTO.setEmail(username + "@wellsfargo.com");
        sessionDTO.setAccountId("ACCT_" + username.hashCode());
        sessionDTO.setCreatedAt(now);
        sessionDTO.setExpiresAt(expiresAt);
        
        // Audit log
        auditLogger.info("AUTH_EVENT | Type: LOGIN_SUCCESS | User: {} | SessionId: {} | IP: {} | UserAgent: {}",
            username, newSessionId, clientIp, truncateUserAgent(userAgent));
        
        logger.info("User logged in successfully: {} with session: {}", username, newSessionId);
        return sessionDTO;
    }
    
    /**
     * Logs out user and invalidates session securely.
     */
    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            logger.warn("Logout called but no session exists");
            return;
        }
        
        String username = (String) session.getAttribute(SESSION_USER_KEY);
        String sessionId = session.getId();
        String clientIp = getClientIp(request);
        
        // Audit log before invalidation
        auditLogger.info("AUTH_EVENT | Type: LOGOUT | User: {} | SessionId: {} | IP: {}",
            username, sessionId, clientIp);
        
        // Invalidate session
        session.invalidate();
        
        logger.info("User logged out: {} session: {}", username, sessionId);
    }
    
    /**
     * Gets current session information.
     */
    public SessionDTO getSession(HttpSession session) {
        if (session == null) {
            return null;
        }
        
        String username = (String) session.getAttribute(SESSION_USER_KEY);
        if (username == null) {
            return null;
        }
        
        Boolean authenticated = (Boolean) session.getAttribute(SESSION_AUTHENTICATED_KEY);
        if (authenticated == null || !authenticated) {
            return null;
        }
        
        // Update last access time
        session.setAttribute(SESSION_LAST_ACCESS_KEY, System.currentTimeMillis());
        
        SessionDTO sessionDTO = new SessionDTO();
        sessionDTO.setSessionId(session.getId());
        sessionDTO.setUsername(username);
        sessionDTO.setEmail((String) session.getAttribute(SESSION_EMAIL_KEY));
        sessionDTO.setAccountId((String) session.getAttribute(SESSION_ACCOUNT_KEY));
        
        // Calculate times
        Long loginTime = (Long) session.getAttribute(SESSION_LOGIN_TIME_KEY);
        if (loginTime != null) {
            sessionDTO.setCreatedAt(LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(loginTime), java.time.ZoneId.systemDefault()));
            sessionDTO.setExpiresAt(sessionDTO.getCreatedAt().plusSeconds(SESSION_TIMEOUT_SECONDS));
        }
        
        return sessionDTO;
    }
    
    /**
     * Validates if session is still valid and not tampered.
     */
    public boolean isSessionValid(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        
        // Check authenticated flag
        Boolean authenticated = (Boolean) session.getAttribute(SESSION_AUTHENTICATED_KEY);
        if (authenticated == null || !authenticated) {
            return false;
        }
        
        // Check username exists
        String username = (String) session.getAttribute(SESSION_USER_KEY);
        if (username == null || username.isEmpty()) {
            return false;
        }
        
        // Optional: Check if client IP changed (session hijacking detection)
        // This can be disabled if users are behind dynamic proxies
        String storedIp = (String) session.getAttribute(SESSION_CLIENT_IP_KEY);
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
     * Gets client IP address, handling proxies.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
    
    /**
     * Truncates user agent for logging.
     */
    private String truncateUserAgent(String userAgent) {
        if (userAgent == null) {
            return "unknown";
        }
        return userAgent.length() > 100 ? userAgent.substring(0, 100) + "..." : userAgent;
    }
}


