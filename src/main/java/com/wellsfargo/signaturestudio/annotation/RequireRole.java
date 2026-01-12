package com.wellsfargo.signaturestudio.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation for role-based access control.
 *
 * Apply this annotation to controller methods to enforce that the user
 * has at least one of the specified roles in their session before the method is invoked.
 *
 * Usage:
 * <pre>
 * {@code @RequireRole("ADMIN")}
 * public ResponseEntity<?> adminOnlyMethod() { ... }
 *
 * {@code @RequireRole({"ADMIN", "MANAGER"})} // User needs either ADMIN or MANAGER
 * public ResponseEntity<?> privilegedMethod() { ... }
 * </pre>
 *
 * If the user doesn't have any of the required roles, an UnauthorizedAccessException
 * will be thrown before the method is invoked.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /**
     * The required role(s). User must have at least one of these roles.
     * @return array of required role names
     */
    String[] value();

    /**
     * Optional message to include in the exception if access is denied.
     * @return custom error message
     */
    String message() default "Access denied: insufficient permissions";

    /**
     * Human-readable description of the operation this annotation protects.
     * Used by the permission registry for documentation and UI rendering.
     * @return operation description (e.g., "View account settings", "Create new account")
     */
    String operation() default "";
}
