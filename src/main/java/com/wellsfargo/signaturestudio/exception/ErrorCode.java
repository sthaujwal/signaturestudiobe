package com.wellsfargo.signaturestudio.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // Validation Errors (400)
    VALIDATION_ERROR("VAL_001", "Validation error", HttpStatus.BAD_REQUEST),
    INVALID_INPUT("VAL_002", "Invalid input provided", HttpStatus.BAD_REQUEST),
    MISSING_REQUIRED_FIELD("VAL_003", "Required field is missing", HttpStatus.BAD_REQUEST),
    
    // Authentication Errors (401)
    UNAUTHORIZED("AUTH_001", "Unauthorized access", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS("AUTH_002", "Invalid credentials", HttpStatus.UNAUTHORIZED),
    SESSION_EXPIRED("AUTH_003", "Session has expired", HttpStatus.UNAUTHORIZED),
    
    // Authorization Errors (403)
    FORBIDDEN("AUTH_004", "Access forbidden", HttpStatus.FORBIDDEN),
    ACCESS_DENIED("AUTH_006", "Access denied", HttpStatus.FORBIDDEN),
    INSUFFICIENT_PERMISSIONS("AUTH_005", "Insufficient permissions", HttpStatus.FORBIDDEN),
    
    // Not Found Errors (404)
    RESOURCE_NOT_FOUND("NOT_FOUND_001", "Resource not found", HttpStatus.NOT_FOUND),
    TRANSACTION_NOT_FOUND("NOT_FOUND_002", "Transaction not found", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND("NOT_FOUND_003", "User not found", HttpStatus.NOT_FOUND),
    
    // Conflict Errors (409)
    RESOURCE_CONFLICT("CONFLICT_001", "Resource conflict", HttpStatus.CONFLICT),
    DUPLICATE_RESOURCE("CONFLICT_002", "Duplicate resource", HttpStatus.CONFLICT),
    
    // Service Integration Errors (502, 503)
    EXTERNAL_SERVICE_ERROR("EXT_001", "External service error", HttpStatus.BAD_GATEWAY),
    ESIGNATURE_SERVICE_ERROR("EXT_002", "eSignature service error", HttpStatus.BAD_GATEWAY),
    ALERT_SERVICE_ERROR("EXT_003", "Alert service error", HttpStatus.BAD_GATEWAY),
    BRANDING_SERVICE_ERROR("EXT_004", "Branding service error", HttpStatus.BAD_GATEWAY),
    SERVICE_UNAVAILABLE("EXT_005", "Service unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    
    // Internal Server Errors (500)
    INTERNAL_ERROR("INT_001", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    DATABASE_ERROR("INT_002", "Database error", HttpStatus.INTERNAL_SERVER_ERROR),
    UNEXPECTED_ERROR("INT_003", "An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    
    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
    
    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}

