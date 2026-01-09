package com.wellsfargo.signaturestudio.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a user attempts to access a resource without proper authorization.
 * This exception is typically thrown by the RoleCheckAspect when @RequireRole validation fails.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedAccessException extends RuntimeException {

    private final String requiredRole;
    private final String userRole;

    public UnauthorizedAccessException(String message) {
        super(message);
        this.requiredRole = null;
        this.userRole = null;
    }

    public UnauthorizedAccessException(String message, String requiredRole, String userRole) {
        super(message);
        this.requiredRole = requiredRole;
        this.userRole = userRole;
    }

    public String getRequiredRole() {
        return requiredRole;
    }

    public String getUserRole() {
        return userRole;
    }
}
