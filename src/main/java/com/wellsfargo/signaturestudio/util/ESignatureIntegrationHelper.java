package com.wellsfargo.signaturestudio.util;

import com.wellsfargo.signaturestudio.exception.ErrorCode;
import com.wellsfargo.signaturestudio.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Helper class for eSignature service integration patterns.
 * Reduces code duplication and cyclomatic complexity.
 */
public final class ESignatureIntegrationHelper {
    
    private static final Logger logger = LoggerFactory.getLogger(ESignatureIntegrationHelper.class);
    
    private ESignatureIntegrationHelper() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Executes an eSignature service operation with consistent error handling.
     * 
     * @param operation Description of the operation for logging
     * @param serviceCall The actual service call to execute
     * @param resourceId Resource identifier for logging
     * @param <T> Return type of the service call
     * @return Result of the service call
     * @throws ServiceException if the service call fails
     */
    public static <T> T executeWithErrorHandling(
            String operation,
            Supplier<T> serviceCall,
            String resourceId) {
        try {
            T result = serviceCall.get();
            logger.info("{} completed successfully: {}", operation, resourceId);
            return result;
        } catch (Exception e) {
            logger.error("Failed to {}: {}", operation, resourceId, e);
            throw new ServiceException(ErrorCode.ESIGNATURE_SERVICE_ERROR, 
                "Failed to " + operation.toLowerCase(), e);
        }
    }
    
    /**
     * Executes an eSignature service operation that returns void.
     */
    public static void executeVoidWithErrorHandling(
            String operation,
            Runnable serviceCall,
            String resourceId) {
        executeWithErrorHandling(operation, () -> {
            serviceCall.run();
            return null;
        }, resourceId);
    }
    
    /**
     * Executes an eSignature service operation with fallback to local data.
     * 
     * @param operation Description of the operation
     * @param serviceCall The service call to execute
     * @param fallbackSupplier Fallback supplier if service call fails
     * @param resourceId Resource identifier for logging
     * @param <T> Return type
     * @return Result from service or fallback
     */
    public static <T> T executeWithFallback(
            String operation,
            Supplier<T> serviceCall,
            Supplier<T> fallbackSupplier,
            String resourceId) {
        try {
            T result = serviceCall.get();
            logger.info("{} completed successfully: {}", operation, resourceId);
            return result;
        } catch (Exception e) {
            logger.warn("Failed to {}: {}, using fallback", operation, resourceId, e);
            return fallbackSupplier.get();
        }
    }
}

