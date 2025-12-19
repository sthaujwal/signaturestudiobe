package com.wellsfargo.signaturestudio.util;

import com.wellsfargo.signaturestudio.dto.AddUserRequest;
import com.wellsfargo.signaturestudio.enums.AuthType;
import com.wellsfargo.signaturestudio.enums.ExternalIdType;
import com.wellsfargo.signaturestudio.exception.ErrorCode;
import com.wellsfargo.signaturestudio.exception.ServiceException;

/**
 * Validator for AddUserRequest.
 * Uses ValidationHelper to ensure required fields are present and valid.
 */
public final class AddRequestValidator {
    
    private AddRequestValidator() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Validates AddUserRequest for required fields and constraints.
     * 
     * Validates:
     * - firstName is present
     * - lastName is present
     * - externalId is present
     * - externalIdType is present and is either "AD-ENT" or "ECN"
     * - authType is present and is either "OLB", "OLX", "AD_ENT", or "OTHER"
     * 
     * @param request The AddUserRequest to validate
     * @throws ServiceException if validation fails
     */
    public static void validate(AddUserRequest request) {
        if (request == null) {
            throw new ServiceException(ErrorCode.VALIDATION_ERROR, "AddUserRequest cannot be null");
        }
        
        // Validate firstName is present
        ValidationHelper.requireNonEmpty(request.getFirstName(), "firstName");
        
        // Validate lastName is present
        ValidationHelper.requireNonEmpty(request.getLastName(), "lastName");
        
        // Validate externalId is present
        ValidationHelper.requireNonEmpty(request.getExternalId(), "externalId");
        
        // Validate externalIdType is present
        String externalIdTypeValue = ValidationHelper.requireNonEmpty(
            request.getExternalIdType(), "externalIdType");
        
        // Validate externalIdType is either AD-ENT or ECN
        ExternalIdType externalIdType = ExternalIdType.fromValue(externalIdTypeValue);
        if (externalIdType == null) {
            throw new ServiceException(ErrorCode.VALIDATION_ERROR, 
                String.format("externalIdType must be one of: %s, but was: '%s'",
                    getExternalIdTypeValues(), externalIdTypeValue));
        }
        
        // Validate authType is present
        String authTypeValue = ValidationHelper.requireNonEmpty(request.getAuthType(), "authType");
        
        // Validate authType is either OLB, OLX, AD_ENT, or OTHER
        AuthType authType = AuthType.fromValue(authTypeValue);
        if (authType == null) {
            throw new ServiceException(ErrorCode.VALIDATION_ERROR, 
                String.format("authType must be one of: %s, but was: '%s'",
                    getAuthTypeValues(), authTypeValue));
        }
    }
    
    /**
     * Gets comma-separated list of valid ExternalIdType values for error messages.
     */
    private static String getExternalIdTypeValues() {
        StringBuilder sb = new StringBuilder();
        ExternalIdType[] values = ExternalIdType.values();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("'").append(values[i].getValue()).append("'");
        }
        return sb.toString();
    }
    
    /**
     * Gets comma-separated list of valid AuthType values for error messages.
     */
    private static String getAuthTypeValues() {
        StringBuilder sb = new StringBuilder();
        AuthType[] values = AuthType.values();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("'").append(values[i].getValue()).append("'");
        }
        return sb.toString();
    }
}

