package com.wellsfargo.signaturestudio.service;

import com.wellsfargo.signaturestudio.constants.SessionConstants;
import com.wellsfargo.signaturestudio.domain.LoginRequest;
import com.wellsfargo.signaturestudio.domain.Session;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

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
    
    private final AccountService accountService;
    
    public AuthenticationService(AccountService accountService) {
        this.accountService = accountService;
    }
    
    /**
     * Authenticates user and creates secure session.
     * Implements session fixation protection by creating new session.
     */
    public Session login(LoginRequest loginRequest, HttpServletRequest request) {
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
        
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(SessionConstants.SESSION_TIMEOUT_SECONDS);
        
        // Get default account for user
        com.wellsfargo.signaturestudio.domain.Account defaultAccount = accountService.getDefaultAccount(username);
        String accountId = defaultAccount.getAccountId();
        
        // Store secure session attributes
        session.setAttribute(SessionConstants.USERNAME, username);
        session.setAttribute(SessionConstants.EMAIL, username + "@wellsfargo.com");
        session.setAttribute(SessionConstants.ACCOUNT_ID, accountId);
        session.setAttribute(SessionConstants.AUTHENTICATED, true);
        session.setAttribute(SessionConstants.LOGIN_TIME, System.currentTimeMillis());
        session.setAttribute(SessionConstants.LAST_ACCESS_TIME, System.currentTimeMillis());
        session.setAttribute(SessionConstants.CLIENT_IP, clientIp);
        session.setAttribute(SessionConstants.USER_AGENT, userAgent);
        session.setMaxInactiveInterval(SessionConstants.SESSION_TIMEOUT_SECONDS);
        
        // Build response DTO
        Session sessionDTO = new Session();
        sessionDTO.setSessionId(newSessionId);
        sessionDTO.setUsername(username);
        sessionDTO.setEmail(username + "@wellsfargo.com");
        sessionDTO.setAccountId(accountId);
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
        
        String username = (String) session.getAttribute(SessionConstants.USERNAME);
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
     * Gets client IP address, handling proxies.
     * Used during login for session security tracking.
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


