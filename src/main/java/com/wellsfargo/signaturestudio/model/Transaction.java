package com.wellsfargo.signaturestudio.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transaction_metadata")
public class Transaction {
    @Id
    private String id;
    
    @Column(length = 500)
    private String title;
    
    @Column(columnDefinition = "CLOB")
    private String description;
    
    @Column(length = 50)
    private String status; // pending, in-progress, completed, rejected
    
    @Column(name = "created_by", length = 255)
    private String createdBy; // Keep for backward compatibility
    
    @Column(name = "creator_username", length = 255)
    private String creatorUsername;
    
    @Column(name = "creator_email", length = 255)
    private String creatorEmail;
    
    @Column(name = "account_id", length = 255)
    private String accountId;
    
    @Column(name = "account_code", length = 255)
    private String accountCode;
    
    @Column(name = "esignature_transaction_id", length = 255)
    private String eSignatureTransactionId;
    
    @Column(name = "document_url", length = 1000)
    private String documentUrl;
    
    @Column(name = "due_date")
    private Instant dueDate;
    
    @Column(length = 20)
    private String priority; // low, medium, high
    
    @Column(name = "email_template", length = 100)
    private String emailTemplate;
    
    @Column(name = "system_of_record", length = 100)
    private String systemOfRecord;
    
    @Column(name = "form_type", length = 100)
    private String formType; // contract, agreement, application, disclosure, etc.
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<User> users = new ArrayList<>();
    
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Document> documents = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
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
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public String getCreatorUsername() {
        return creatorUsername;
    }
    
    public void setCreatorUsername(String creatorUsername) {
        this.creatorUsername = creatorUsername;
    }
    
    public String getCreatorEmail() {
        return creatorEmail;
    }
    
    public void setCreatorEmail(String creatorEmail) {
        this.creatorEmail = creatorEmail;
    }
    
    public String getAccountId() {
        return accountId;
    }
    
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
    
    public String getAccountCode() {
        return accountCode;
    }
    
    public void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }
    
    public String getESignatureTransactionId() {
        return eSignatureTransactionId;
    }
    
    public void setESignatureTransactionId(String eSignatureTransactionId) {
        this.eSignatureTransactionId = eSignatureTransactionId;
    }
    
    public String getDocumentUrl() {
        return documentUrl;
    }
    
    public void setDocumentUrl(String documentUrl) {
        this.documentUrl = documentUrl;
    }
    
    public Instant getDueDate() {
        return dueDate;
    }
    
    public void setDueDate(Instant dueDate) {
        this.dueDate = dueDate;
    }
    
    public String getPriority() {
        return priority;
    }
    
    public void setPriority(String priority) {
        this.priority = priority;
    }
    
    public String getEmailTemplate() {
        return emailTemplate;
    }
    
    public void setEmailTemplate(String emailTemplate) {
        this.emailTemplate = emailTemplate;
    }
    
    public String getSystemOfRecord() {
        return systemOfRecord;
    }
    
    public void setSystemOfRecord(String systemOfRecord) {
        this.systemOfRecord = systemOfRecord;
    }
    
    public String getFormType() {
        return formType;
    }
    
    public void setFormType(String formType) {
        this.formType = formType;
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
    
    public List<User> getUsers() {
        return users;
    }
    
    public void setUsers(List<User> users) {
        this.users = users;
    }
    
    public List<Document> getDocuments() {
        return documents;
    }
    
    public void setDocuments(List<Document> documents) {
        this.documents = documents;
    }
}

