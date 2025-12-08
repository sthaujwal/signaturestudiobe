package com.wellsfargo.signaturestudio.util;

import com.wellsfargo.signaturestudio.exception.ErrorCode;
import com.wellsfargo.signaturestudio.exception.ServiceException;

import java.time.LocalDateTime;

/**
 * Utility class for common validation operations.
 * Reduces cyclomatic complexity by extracting validation logic.
 */
public final class ValidationHelper {
    
    private ValidationHelper() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Validates that a value is not null.
     */
    public static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new ServiceException(ErrorCode.VALIDATION_ERROR, 
                fieldName + " is required");
        }
        return value;
    }
    
    /**
     * Validates that a string is not null or empty.
     */
    public static String requireNonEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ServiceException(ErrorCode.VALIDATION_ERROR, 
                fieldName + " is required");
        }
        return value;
    }
    
    /**
     * Validates date range - end date must be after start date.
     */
    public static void validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null) {
            throw new ServiceException(ErrorCode.VALIDATION_ERROR, "Start date is required");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new ServiceException(ErrorCode.VALIDATION_ERROR, 
                "End date must be after start date");
        }
    }
    
    /**
     * Validates that a resource belongs to a transaction.
     */
    public static void validateResourceBelongsToTransaction(
            String resourceId, 
            String resourceTransactionId, 
            String expectedTransactionId,
            String resourceType) {
        if (!expectedTransactionId.equals(resourceTransactionId)) {
            throw new ServiceException(ErrorCode.RESOURCE_CONFLICT, 
                String.format("%s %s does not belong to transaction %s", 
                    resourceType, resourceId, expectedTransactionId));
        }
    }
}

