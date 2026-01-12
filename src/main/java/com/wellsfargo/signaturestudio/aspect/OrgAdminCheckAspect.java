package com.wellsfargo.signaturestudio.aspect;

import com.wellsfargo.signaturestudio.annotation.RequireOrgAdmin;
import com.wellsfargo.signaturestudio.constants.SessionConstants;
import com.wellsfargo.signaturestudio.exception.UnauthorizedAccessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * AspectJ aspect for enforcing organization admin access control using @RequireOrgAdmin annotation.
 *
 * This aspect intercepts methods annotated with @RequireOrgAdmin and validates that
 * the user's session has the ORG_ADMIN flag set before allowing method execution.
 *
 * Security flow:
 * 1. Intercepts method call with @RequireOrgAdmin annotation
 * 2. Retrieves the current HTTP session
 * 3. Checks if user is authenticated
 * 4. Validates user has IS_ORG_ADMIN = true in session
 * 5. Proceeds with method execution or throws UnauthorizedAccessException
 */
@Aspect
@Component
public class OrgAdminCheckAspect {

    private static final Logger logger = LoggerFactory.getLogger(OrgAdminCheckAspect.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("SECURITY_AUDIT");

    /**
     * Intercepts methods annotated with @RequireOrgAdmin.
     * Validates ORG_ADMIN requirement before allowing method execution.
     */
    @Around("@annotation(com.wellsfargo.signaturestudio.annotation.RequireOrgAdmin)")
    public Object checkOrgAdmin(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequireOrgAdmin requireOrgAdmin = method.getAnnotation(RequireOrgAdmin.class);

        if (requireOrgAdmin == null) {
            // Shouldn't happen, but proceed if annotation not found
            return joinPoint.proceed();
        }

        String customMessage = requireOrgAdmin.message();

        // Get HTTP session
        HttpSession session = getHttpSession();
        if (session == null) {
            auditLogger.warn("ORG_ADMIN_CHECK_FAILED | Method: {} | Reason: No session found",
                    method.getName());
            throw new UnauthorizedAccessException("No active session. Please log in.");
        }

        // Check authentication
        Boolean isAuthenticated = (Boolean) session.getAttribute(SessionConstants.AUTHENTICATED);
        if (isAuthenticated == null || !isAuthenticated) {
            String username = (String) session.getAttribute(SessionConstants.USERNAME);
            auditLogger.warn("ORG_ADMIN_CHECK_FAILED | Method: {} | User: {} | Reason: Not authenticated",
                    method.getName(), username);
            throw new UnauthorizedAccessException("User is not authenticated.");
        }

        // Check for ORG_ADMIN flag
        Boolean isOrgAdmin = (Boolean) session.getAttribute(SessionConstants.IS_ORG_ADMIN);
        String username = (String) session.getAttribute(SessionConstants.USERNAME);

        if (!Boolean.TRUE.equals(isOrgAdmin)) {
            auditLogger.warn("ORG_ADMIN_CHECK_FAILED | Method: {} | User: {} | Reason: Not an organization admin",
                    method.getName(), username);

            String errorMessage = customMessage != null && !customMessage.isEmpty()
                    ? customMessage
                    : "Access denied. Organization admin privileges required.";

            throw new UnauthorizedAccessException(errorMessage);
        }

        // ORG_ADMIN check passed, log success and proceed
        logger.debug("ORG_ADMIN_CHECK_SUCCESS | Method: {} | User: {}",
                method.getName(), username);

        auditLogger.info("ORG_ADMIN_ACCESS | Method: {} | User: {} | Operation: {}",
                method.getName(), username,
                requireOrgAdmin.operation().isEmpty() ? "N/A" : requireOrgAdmin.operation());

        return joinPoint.proceed();
    }

    /**
     * Retrieves the HTTP session from the current request context.
     * Returns null if no request context or session is available.
     */
    private HttpSession getHttpSession() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                if (request != null) {
                    return request.getSession(false);
                }
            }
        } catch (Exception e) {
            logger.error("Error retrieving HTTP session for ORG_ADMIN check", e);
        }
        return null;
    }
}
