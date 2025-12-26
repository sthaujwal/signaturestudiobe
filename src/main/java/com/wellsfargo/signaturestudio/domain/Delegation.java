package com.wellsfargo.signaturestudio.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public class Delegation {
    private String id;
    
    @NotBlank(message = "Delegator user ID is required")
    @Size(max = 255, message = "Delegator user ID must not exceed 255 characters")
    private String delegatorUserId;
    
    @NotBlank(message = "Delegate user ID is required")
    @Size(max = 255, message = "Delegate user ID must not exceed 255 characters")
    private String delegateUserId;
    
    @Size(max = 255, message = "Delegator email must not exceed 255 characters")
    private String delegatorEmail;
    
    @Size(max = 255, message = "Delegate email must not exceed 255 characters")
    private String delegateEmail;
    
    @Size(max = 100, message = "Reason must not exceed 100 characters")
    private String reason; // PTO, left_company, other
    
    private String description;
    
    @NotNull(message = "Start date is required")
    private Instant startDate;
    
    private Instant endDate; // null for permanent delegations
    
    private String status; // active, expired, cancelled
    
    @Size(max = 255, message = "Account ID must not exceed 255 characters")
    private String accountId;
    
    private String createdBy;
    
    private Instant createdAt;
    
    private Instant updatedAt;
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getDelegatorUserId() {
        return delegatorUserId;
    }
    
    public void setDelegatorUserId(String delegatorUserId) {
        this.delegatorUserId = delegatorUserId;
    }
    
    public String getDelegateUserId() {
        return delegateUserId;
    }
    
    public void setDelegateUserId(String delegateUserId) {
        this.delegateUserId = delegateUserId;
    }
    
    public String getDelegatorEmail() {
        return delegatorEmail;
    }
    
    public void setDelegatorEmail(String delegatorEmail) {
        this.delegatorEmail = delegatorEmail;
    }
    
    public String getDelegateEmail() {
        return delegateEmail;
    }
    
    public void setDelegateEmail(String delegateEmail) {
        this.delegateEmail = delegateEmail;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Instant getStartDate() {
        return startDate;
    }
    
    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }
    
    public Instant getEndDate() {
        return endDate;
    }
    
    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getAccountId() {
        return accountId;
    }
    
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

