package com.wellsfargo.signaturestudio.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.web.session.HttpSessionCreatedEvent;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.session.events.SessionCreatedEvent;
import org.springframework.session.events.SessionDeletedEvent;
import org.springframework.stereotype.Component;

/**
 * Listens to session lifecycle events for audit logging.
 *
 * NEW APPROACH (Session Attributes):
 * - Access tokens stored in SPRING_SESSION_ATTRIBUTES
 * - Spring automatically deletes session attributes when session is deleted/expired
 * - No manual token cleanup needed
 *
 * This listener only handles audit logging now (not token cleanup).
 */
@Component
public class SessionEventListener {

    private static final Logger logger = LoggerFactory.getLogger(SessionEventListener.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("SECURITY_AUDIT");

    /**
     * Handles Spring Session created event (audit logging).
     */
    @EventListener
    public void onSessionCreated(SessionCreatedEvent event) {
        String sessionId = event.getSessionId();
        auditLogger.info("SESSION_EVENT | Type: CREATED | SessionId: {}", sessionId);
        logger.debug("Session created: {}", sessionId);
    }

    /**
     * Handles Spring Session deleted event (explicit logout).
     *
     * Note: Access token (session attribute) is automatically deleted by Spring.
     * No manual cleanup needed.
     */
    @EventListener
    public void onSessionDeleted(SessionDeletedEvent event) {
        String sessionId = event.getSessionId();
        auditLogger.info("SESSION_EVENT | Type: DELETED | SessionId: {}", sessionId);
        logger.info("Session deleted (logout) - access token auto-removed with session attributes");
    }

    /**
     * Handles HTTP Session created event (low-level event).
     */
    @EventListener
    public void onHttpSessionCreated(HttpSessionCreatedEvent event) {
        String sessionId = event.getSession().getId();
        logger.debug("HTTP Session created: {}", sessionId);
    }

    /**
     * Handles HTTP Session destroyed event (low-level event).
     */
    @EventListener
    public void onHttpSessionDestroyed(HttpSessionDestroyedEvent event) {
        String sessionId = event.getSession().getId();
        logger.debug("HTTP Session destroyed: {}", sessionId);
    }
}

