package com.wellsfargo.signaturestudio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class TransactionDTO {
    private String id;
    
    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title must not exceed 500 characters")
    private String title;
    
    private String description;
    
    private String status;
    
    private String createdBy; // Keep for backward compatibility
    
    private String creatorUsername;
    
    private String creatorEmail;
    
    private String accountId;
    
    private String accountCode;
    
    private String eSignatureTransactionId;
    
    private String documentUrl;
    
    private Instant dueDate;
    
    private String priority;
    
    private String emailTemplate;
    
    private String systemOfRecord;
    
    private String formType; // contract, agreement, application, disclosure, etc.
    
    private Instant createdAt;
    
    private Instant updatedAt;
    
    private List<UserDTO> users;
    
    private List<DocumentDTO> documents;
    
    private Map<String, String> customAttributes;
    
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
    
    public List<UserDTO> getUsers() {
        return users;
    }
    
    public void setUsers(List<UserDTO> users) {
        this.users = users;
    }
    
    public List<DocumentDTO> getDocuments() {
        return documents;
    }
    
    public void setDocuments(List<DocumentDTO> documents) {
        this.documents = documents;
    }
    
    public Map<String, String> getCustomAttributes() {
        return customAttributes;
    }
    
    public void setCustomAttributes(Map<String, String> customAttributes) {
        this.customAttributes = customAttributes;
    }
}


