package com.wellsfargo.signaturestudio.exception;

/**
 * Abstract base exception class for all application exceptions.
 * Provides common exception handling structure.
 */
public abstract class AbstractException extends RuntimeException {
    
    protected AbstractException(String message) {
        super(message);
    }
    
    protected AbstractException(String message, Throwable cause) {
        super(message, cause);
    }
    
    protected AbstractException(Throwable cause) {
        super(cause);
    }
}

