package com.wellsfargo.signaturestudio.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "delegations")
public class Delegation {
    @Id
    private String id;
    
    @Column(name = "delegator_user_id", length = 255, nullable = false)
    private String delegatorUserId; // User who is delegating
    
    @Column(name = "delegate_user_id", length = 255, nullable = false)
    private String delegateUserId; // User who receives the delegation
    
    @Column(name = "delegator_email", length = 255)
    private String delegatorEmail;
    
    @Column(name = "delegate_email", length = 255)
    private String delegateEmail;
    
    @Column(name = "reason", length = 100)
    private String reason; // PTO, left_company, other
    
    @Column(name = "description", columnDefinition = "CLOB")
    private String description; // Additional details
    
    @Column(name = "start_date", nullable = false)
    private Instant startDate;
    
    @Column(name = "end_date")
    private Instant endDate; // null for permanent delegations (left company)
    
    @Column(name = "status", length = 50)
    private String status; // active, expired, cancelled
    
    @Column(name = "account_id", length = 255)
    private String accountId; // Optional: scope to specific account
    
    @Column(name = "created_by", length = 255)
    private String createdBy; // Who created this delegation record
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status == null) {
            status = "active";
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
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

