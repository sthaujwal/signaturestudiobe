package com.wellsfargo.signaturestudio.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation for organization admin access control.
 *
 * Apply this annotation to controller methods to enforce that the user
 * has the ORG_ADMIN role before the method is invoked.
 *
 * Usage:
 * <pre>
 * {@code @RequireOrgAdmin}
 * public ResponseEntity<?> orgAdminOnlyMethod() { ... }
 *
 * {@code @RequireOrgAdmin(operation = "Create new account")}
 * public ResponseEntity<?> createAccount() { ... }
 * </pre>
 *
 * If the user doesn't have ORG_ADMIN privileges, an UnauthorizedAccessException
 * will be thrown before the method is invoked.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireOrgAdmin {

    /**
     * Optional message to include in the exception if access is denied.
     * @return custom error message
     */
    String message() default "Access denied: Organization admin privileges required";

    /**
     * Human-readable description of the operation this annotation protects.
     * Used by the permission registry for documentation and UI rendering.
     * @return operation description (e.g., "Create new account", "View all accounts")
     */
    String operation() default "";
}
