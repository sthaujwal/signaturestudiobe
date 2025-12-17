package com.wellsfargo.signaturestudio.service;

import com.wellsfargo.signaturestudio.constants.SessionConstants;
import com.wellsfargo.signaturestudio.dto.SessionDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
     * @return SessionDTO with session information, or null if session is invalid
     */
    public SessionDTO getSessionInfo(HttpSession session) {
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
        
        SessionDTO sessionDTO = new SessionDTO();
        sessionDTO.setSessionId(session.getId());
        sessionDTO.setUsername(username);
        sessionDTO.setEmail(getEmail(session));
        sessionDTO.setAccountId(getAccountId(session));
        
        // Calculate times
        Long loginTime = (Long) session.getAttribute(SessionConstants.LOGIN_TIME);
        if (loginTime != null) {
            sessionDTO.setCreatedAt(LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(loginTime), java.time.ZoneId.systemDefault()));
            sessionDTO.setExpiresAt(sessionDTO.getCreatedAt().plusSeconds(SessionConstants.SESSION_TIMEOUT_SECONDS));
        }
        
        return sessionDTO;
    }
    
    /**
     * Gets current session information from HttpServletRequest.
     * 
     * @param request The HTTP request
     * @return SessionDTO with session information, or null if session is invalid
     */
    public SessionDTO getSessionInfo(HttpServletRequest request) {
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
}

