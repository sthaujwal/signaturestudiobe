package com.wellsfargo.signaturestudio.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validation annotation to prevent XSS attacks.
 * Rejects strings containing potentially dangerous HTML/script tags.
 *
 * Usage:
 * <pre>
 * public class CreateAccountRequest {
 *     @NoXss
 *     @NotBlank
 *     private String accountName;
 * }
 * </pre>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NoXssValidator.class)
@Documented
public @interface NoXss {
    String message() default "Input contains potentially unsafe content";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
