package com.wellsfargo.signaturestudio.config;

import com.wellsfargo.signaturestudio.service.AuthenticationTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.web.session.HttpSessionCreatedEvent;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.session.events.SessionCreatedEvent;
import org.springframework.session.events.SessionDeletedEvent;
import org.springframework.session.events.SessionExpiredEvent;
import org.springframework.stereotype.Component;

/**
 * Listens to session lifecycle events for audit logging and automatic token cleanup.
 *
 * When sessions are destroyed (logout, timeout, etc.), this listener automatically
 * revokes all associated authentication tokens (both authorization codes and access tokens).
 */
@Component
public class SessionEventListener {

    private static final Logger logger = LoggerFactory.getLogger(SessionEventListener.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("SECURITY_AUDIT");

    private final AuthenticationTokenService tokenService;

    public SessionEventListener(AuthenticationTokenService tokenService) {
        this.tokenService = tokenService;
    }
    
    /**
     * Handles Spring Session created event.
     */
    @EventListener
    public void onSessionCreated(SessionCreatedEvent event) {
        String sessionId = event.getSessionId();
        auditLogger.info("SESSION_EVENT | Type: CREATED | SessionId: {}", sessionId);
        logger.debug("Session created: {}", sessionId);
    }
    
    /**
     * Handles Spring Session deleted event (explicit logout).
     * CRITICAL: Automatically revokes all tokens for this session.
     */
    @EventListener
    public void onSessionDeleted(SessionDeletedEvent event) {
        String sessionId = event.getSessionId();
        auditLogger.info("SESSION_EVENT | Type: DELETED | SessionId: {}", sessionId);
        logger.info("Session deleted, revoking all tokens: {}", sessionId);

        // Revoke all tokens (authorization codes and access tokens) for this session
        tokenService.revokeTokensForSession(sessionId);
    }

    /**
     * Handles Spring Session expired event (timeout).
     * CRITICAL: Automatically revokes all tokens for this session.
     */
    @EventListener
    public void onSessionExpired(SessionExpiredEvent event) {
        String sessionId = event.getSessionId();
        auditLogger.warn("SESSION_EVENT | Type: EXPIRED | SessionId: {}", sessionId);
        logger.info("Session expired, revoking all tokens: {}", sessionId);

        // Revoke all tokens (authorization codes and access tokens) for this session
        tokenService.revokeTokensForSession(sessionId);
    }
    
    /**
     * Handles HTTP Session created event.
     */
    @EventListener
    public void onHttpSessionCreated(HttpSessionCreatedEvent event) {
        String sessionId = event.getSession().getId();
        logger.debug("HTTP Session created: {}", sessionId);
    }
    
    /**
     * Handles HTTP Session destroyed event.
     */
    @EventListener
    public void onHttpSessionDestroyed(HttpSessionDestroyedEvent event) {
        String sessionId = event.getSession().getId();
        logger.debug("HTTP Session destroyed: {}", sessionId);
    }
}

