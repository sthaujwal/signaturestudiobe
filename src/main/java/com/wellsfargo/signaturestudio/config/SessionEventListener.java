package com.wellsfargo.signaturestudio.config;

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
 * Listens to session lifecycle events for audit logging and cleanup.
 */
@Component
public class SessionEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionEventListener.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("SECURITY_AUDIT");
    
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
     */
    @EventListener
    public void onSessionDeleted(SessionDeletedEvent event) {
        String sessionId = event.getSessionId();
        auditLogger.info("SESSION_EVENT | Type: DELETED | SessionId: {}", sessionId);
        logger.debug("Session deleted: {}", sessionId);
    }
    
    /**
     * Handles Spring Session expired event (timeout).
     */
    @EventListener
    public void onSessionExpired(SessionExpiredEvent event) {
        String sessionId = event.getSessionId();
        auditLogger.warn("SESSION_EVENT | Type: EXPIRED | SessionId: {}", sessionId);
        logger.debug("Session expired: {}", sessionId);
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

