package com.wellsfargo.signaturestudio.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponseDTO {
    private String errorCode;
    private String message;
    private String detailMessage;
    private LocalDateTime timestamp;
    private String traceId;
    private Map<String, String> validationErrors;
    private String path;
    
    public ErrorResponseDTO() {
        this.timestamp = LocalDateTime.now();
    }
    
    public ErrorResponseDTO(String errorCode, String message) {
        this();
        this.errorCode = errorCode;
        this.message = message;
    }
    
    public ErrorResponseDTO(String errorCode, String message, String detailMessage) {
        this(errorCode, message);
        this.detailMessage = detailMessage;
    }
    
    // Getters and Setters
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getDetailMessage() {
        return detailMessage;
    }
    
    public void setDetailMessage(String detailMessage) {
        this.detailMessage = detailMessage;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getTraceId() {
        return traceId;
    }
    
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
    
    public Map<String, String> getValidationErrors() {
        return validationErrors;
    }
    
    public void setValidationErrors(Map<String, String> validationErrors) {
        this.validationErrors = validationErrors;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
}

