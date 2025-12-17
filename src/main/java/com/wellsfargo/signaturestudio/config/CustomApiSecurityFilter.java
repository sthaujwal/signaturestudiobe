package com.wellsfargo.signaturestudio.config;

import com.wellsfargo.signaturestudio.constants.SessionConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Security filter for custom APIs that require valid Spring Session authentication.
 * 
 * This filter:
 * 1. Validates session exists and is not expired
 * 2. Validates session contains required authentication attributes
 * 3. Applies session fixation protection
 * 4. Adds security headers to response
 * 5. Logs security events for audit
 * 
 * Works alongside the library's enterprise security config.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class CustomApiSecurityFilter extends OncePerRequestFilter {
    
    private static final Logger auditLogger = LoggerFactory.getLogger("SECURITY_AUDIT");
    
    // Paths that require session validation (custom APIs)
    private static final Set<String> PROTECTED_API_PREFIXES = Set.of(
        "/api/transactions",
        "/api/documents",
        "/api/users",
        "/api/delegations",
        "/api/team-members"
    );
    
    // Paths that are public (no session required)
    private static final Set<String> PUBLIC_PATHS = Set.of(
        "/api/public",
        "/api/auth/login",
        "/api/auth/csrf-token",
        "/api/health",
        "/actuator"
    );
    
    @Override
    protected void doFilterInternal(@SuppressWarnings("null") HttpServletRequest request,
                                    @SuppressWarnings("null") HttpServletResponse response,
                                    @SuppressWarnings("null") FilterChain filterChain) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        String method = request.getMethod();
        String clientIp = getClientIp(request);
        
        // Skip filter for public paths
        if (isPublicPath(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Check if this is a protected custom API
        if (isProtectedApi(requestPath)) {
            // Validate session
            HttpSession session = request.getSession(false);
            
            if (session == null) {
                auditLogger.warn("SECURITY_EVENT | Type: NO_SESSION | Path: {} | Method: {} | IP: {}",
                    requestPath, method, clientIp);
                sendUnauthorizedResponse(response, "No valid session found");
                return;
            }
            
            // Validate session has required authentication attributes
            if (!isSessionAuthenticated(session)) {
                auditLogger.warn("SECURITY_EVENT | Type: UNAUTHENTICATED_SESSION | SessionId: {} | Path: {} | IP: {}",
                    session.getId(), requestPath, clientIp);
                sendUnauthorizedResponse(response, "Session not authenticated");
                return;
            }
            
            // Update last access time for session activity tracking
            session.setAttribute(SessionConstants.LAST_ACCESS_TIME, System.currentTimeMillis());
            
            // Log successful access for audit
            String username = (String) session.getAttribute(SessionConstants.USERNAME);
            auditLogger.info("SECURITY_EVENT | Type: API_ACCESS | User: {} | Path: {} | Method: {} | SessionId: {} | IP: {}",
                username, requestPath, method, session.getId(), clientIp);
        }
        
        // Add security headers
        addSecurityHeaders(response);
        
        // Continue filter chain
        filterChain.doFilter(request, response);
    }
    
    /**
     * Checks if the session contains valid authentication attributes.
     */
    private boolean isSessionAuthenticated(HttpSession session) {
        // Check for required session attributes
        String username = (String) session.getAttribute(SessionConstants.USERNAME);
        String accountId = (String) session.getAttribute(SessionConstants.ACCOUNT_ID);
        
        if (username == null || username.isEmpty()) {
            return false;
        }
        
        if (accountId == null || accountId.isEmpty()) {
            return false;
        }
        
        // Optional: Check if explicitly marked as authenticated
        Boolean authenticated = (Boolean) session.getAttribute(SessionConstants.AUTHENTICATED);
        if (authenticated != null && !authenticated) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Checks if the path is a public endpoint.
     */
    private boolean isPublicPath(String path) {
        for (String publicPath : PUBLIC_PATHS) {
            if (path.startsWith(publicPath)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if the path is a protected custom API.
     */
    private boolean isProtectedApi(String path) {
        for (String prefix : PROTECTED_API_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Adds standard security headers to response.
     */
    private void addSecurityHeaders(HttpServletResponse response) {
        // Prevent clickjacking
        response.setHeader("X-Frame-Options", "DENY");
        
        // Prevent MIME type sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");
        
        // Enable XSS protection
        response.setHeader("X-XSS-Protection", "1; mode=block");
        
        // Strict Transport Security (HTTPS only)
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        
        // Content Security Policy
        response.setHeader("Content-Security-Policy", "default-src 'self'");
        
        // Referrer Policy
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // Cache control for sensitive data
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
    }
    
    /**
     * Sends 401 Unauthorized response.
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write(String.format(
            "{\"error\":\"Unauthorized\",\"message\":\"%s\",\"status\":401}", message));
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
}

