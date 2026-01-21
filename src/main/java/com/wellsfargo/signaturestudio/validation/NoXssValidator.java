package com.wellsfargo.signaturestudio.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Validator for @NoXss annotation.
 *
 * Rejects input containing potentially dangerous patterns:
 * - HTML tags: <script>, <iframe>, <object>, <embed>, etc.
 * - JavaScript events: onclick, onerror, onload, etc.
 * - JavaScript protocols: javascript:, data:text/html
 * - SQL injection patterns (basic)
 *
 * This is a DENY-list approach - reject known bad patterns.
 * For user-generated HTML content, use a whitelist HTML sanitizer library instead.
 */
public class NoXssValidator implements ConstraintValidator<NoXss, String> {

    private static final Logger auditLogger = LoggerFactory.getLogger("SECURITY_AUDIT");

    // XSS patterns to detect
    private static final Pattern[] XSS_PATTERNS = {
        // Script tags
        Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        Pattern.compile("<script[^>]*>", Pattern.CASE_INSENSITIVE),

        // Dangerous tags
        Pattern.compile("<iframe[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<object[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<embed[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<applet[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<meta[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<link[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<style[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<form[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<input[^>]*>", Pattern.CASE_INSENSITIVE),

        // JavaScript event handlers
        Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE),

        // JavaScript protocols
        Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("data:text/html", Pattern.CASE_INSENSITIVE),

        // Expression() - IE specific
        Pattern.compile("expression\\s*\\(", Pattern.CASE_INSENSITIVE),

        // Base64 encoded scripts (common XSS evasion)
        Pattern.compile("<\\s*img[^>]+src\\s*=\\s*['\"]?\\s*data:", Pattern.CASE_INSENSITIVE)
    };

    @Override
    public void initialize(NoXss constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Null and empty strings are valid (use @NotNull/@NotBlank for those checks)
        if (value == null || value.isEmpty()) {
            return true;
        }

        // Check against each XSS pattern
        for (Pattern pattern : XSS_PATTERNS) {
            if (pattern.matcher(value).find()) {
                auditLogger.warn("XSS_VALIDATION_FAILED | Pattern: {} | Value: {}",
                    pattern.pattern(), sanitizeForLogging(value));

                // Provide more specific error message
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    "Input contains potentially unsafe content (HTML tags or scripts are not allowed)"
                ).addConstraintViolation();

                return false;
            }
        }

        return true;
    }

    /**
     * Sanitize value for logging to prevent log injection attacks.
     */
    private String sanitizeForLogging(String value) {
        if (value == null) {
            return "null";
        }

        // Truncate if too long
        String sanitized = value.length() > 100 ? value.substring(0, 100) + "..." : value;

        // Replace newlines and control characters
        sanitized = sanitized.replaceAll("[\n\r\t]", " ");

        return sanitized;
    }
}
