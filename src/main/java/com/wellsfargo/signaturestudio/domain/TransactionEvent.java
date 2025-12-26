package com.wellsfargo.signaturestudio.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;

/**
 * Domain object for transaction events received from ESignatureService via Kafka
 */
public class TransactionEvent {
    
    @JsonProperty("eventType")
    private String eventType; // transaction_created, transaction_updated, transaction_completed, transaction_rejected, etc.
    
    @JsonProperty("transactionId")
    private String transactionId; // eSignature transaction ID
    
    @JsonProperty("bffTransactionId")
    private String bffTransactionId; // BFF transaction ID (if available)
    
    @JsonProperty("status")
    private String status; // pending, in-progress, completed, rejected
    
    @JsonProperty("previousStatus")
    private String previousStatus; // Previous status before change
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("userId")
    private String userId; // User who triggered the event
    
    @JsonProperty("accountId")
    private String accountId;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata; // Additional event metadata
    
    @JsonProperty("errorMessage")
    private String errorMessage; // Error message if event represents an error
    
    // Getters and Setters
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
    public String getBffTransactionId() {
        return bffTransactionId;
    }
    
    public void setBffTransactionId(String bffTransactionId) {
        this.bffTransactionId = bffTransactionId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getPreviousStatus() {
        return previousStatus;
    }
    
    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getAccountId() {
        return accountId;
    }
    
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

