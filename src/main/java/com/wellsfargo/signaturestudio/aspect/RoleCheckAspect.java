package com.wellsfargo.signaturestudio.aspect;

import com.wellsfargo.signaturestudio.annotation.RequireRole;
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
import java.util.Arrays;

/**
 * AspectJ aspect for enforcing role-based access control using @RequireRole annotation.
 *
 * This aspect intercepts methods annotated with @RequireRole and validates that
 * the user's session contains one of the required roles before allowing method execution.
 *
 * Security flow:
 * 1. Intercepts method call with @RequireRole annotation
 * 2. Retrieves the current HTTP session
 * 3. Checks if user is authenticated
 * 4. Validates user has at least one of the required roles
 * 5. Proceeds with method execution or throws UnauthorizedAccessException
 */
@Aspect
@Component
public class RoleCheckAspect {

    private static final Logger logger = LoggerFactory.getLogger(RoleCheckAspect.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("SECURITY_AUDIT");

    /**
     * Intercepts methods annotated with @RequireRole.
     * Validates role requirements before allowing method execution.
     */
    @Around("@annotation(com.wellsfargo.signaturestudio.annotation.RequireRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequireRole requireRole = method.getAnnotation(RequireRole.class);

        if (requireRole == null) {
            // Shouldn't happen, but proceed if annotation not found
            return joinPoint.proceed();
        }

        String[] requiredRoles = requireRole.value();
        String customMessage = requireRole.message();

        // Get HTTP session
        HttpSession session = getHttpSession();
        if (session == null) {
            auditLogger.warn("ROLE_CHECK_FAILED | Method: {} | Reason: No session found | RequiredRoles: {}",
                    method.getName(), Arrays.toString(requiredRoles));
            throw new UnauthorizedAccessException("No active session. Please log in.");
        }

        // Check authentication
        Boolean isAuthenticated = (Boolean) session.getAttribute(SessionConstants.AUTHENTICATED);
        if (isAuthenticated == null || !isAuthenticated) {
            String username = (String) session.getAttribute(SessionConstants.USERNAME);
            auditLogger.warn("ROLE_CHECK_FAILED | Method: {} | User: {} | Reason: Not authenticated | RequiredRoles: {}",
                    method.getName(), username, Arrays.toString(requiredRoles));
            throw new UnauthorizedAccessException("User is not authenticated.");
        }

        // Get user role from session
        String userRole = (String) session.getAttribute(SessionConstants.ROLE);
        String username = (String) session.getAttribute(SessionConstants.USERNAME);

        if (userRole == null || userRole.isEmpty()) {
            auditLogger.warn("ROLE_CHECK_FAILED | Method: {} | User: {} | Reason: No role in session | RequiredRoles: {}",
                    method.getName(), username, Arrays.toString(requiredRoles));
            throw new UnauthorizedAccessException("User has no assigned role.");
        }

        // Check if user has any of the required roles
        boolean hasRequiredRole = Arrays.stream(requiredRoles)
                .anyMatch(role -> role.equalsIgnoreCase(userRole));

        if (!hasRequiredRole) {
            auditLogger.warn("ROLE_CHECK_FAILED | Method: {} | User: {} | UserRole: {} | RequiredRoles: {}",
                    method.getName(), username, userRole, Arrays.toString(requiredRoles));

            String errorMessage = customMessage != null && !customMessage.isEmpty()
                    ? customMessage
                    : String.format("Access denied. Required role(s): %s. User role: %s",
                            Arrays.toString(requiredRoles), userRole);

            throw new UnauthorizedAccessException(
                    errorMessage,
                    Arrays.toString(requiredRoles),
                    userRole
            );
        }

        // Role check passed, log success and proceed
        logger.debug("ROLE_CHECK_SUCCESS | Method: {} | User: {} | UserRole: {} | RequiredRoles: {}",
                method.getName(), username, userRole, Arrays.toString(requiredRoles));

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
            logger.error("Error retrieving HTTP session for role check", e);
        }
        return null;
    }
}
