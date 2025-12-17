package com.wellsfargo.signaturestudio.exception;

/**
 * Service layer exception for business logic errors.
 * Extends AbstractException to follow application exception hierarchy.
 */
public class ServiceException extends AbstractException {
    
    private final ErrorCode errorCode;
    private final String detailMessage;
    
    public ServiceException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detailMessage = null;
    }
    
    public ServiceException(ErrorCode errorCode, String detailMessage) {
        super(errorCode.getMessage() + (detailMessage != null ? ": " + detailMessage : ""));
        this.errorCode = errorCode;
        this.detailMessage = detailMessage;
    }
    
    public ServiceException(ErrorCode errorCode, String detailMessage, Throwable cause) {
        super(errorCode.getMessage() + (detailMessage != null ? ": " + detailMessage : ""), cause);
        this.errorCode = errorCode;
        this.detailMessage = detailMessage;
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
    
    public String getDetailMessage() {
        return detailMessage;
    }
}

