package com.wellsfargo.signaturestudio.dto;

import java.time.LocalDateTime;

public class DocumentDTO {
    private String id;
    private String transactionId;
    private String name;
    private String title;
    private String eSignatureDocumentId; // ID from ESignatureService to fetch details
    
    // Detailed fields (fetched from ESignatureService when needed)
    private String description;
    private String fileName;
    private String originalFileName;
    private String fileType;
    private Long fileSize;
    private String storagePath;
    private String storageUrl;
    private String mimeType;
    private String uploadedBy;
    private String uploadStatus;
    private Integer pageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // These will be populated from ESignatureService when needed
    private java.util.List<FormFieldDTO> formFields;
    private java.util.List<ICMPDTO> icmpObjects;
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getESignatureDocumentId() {
        return eSignatureDocumentId;
    }
    
    public void setESignatureDocumentId(String eSignatureDocumentId) {
        this.eSignatureDocumentId = eSignatureDocumentId;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getOriginalFileName() {
        return originalFileName;
    }
    
    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }
    
    public String getFileType() {
        return fileType;
    }
    
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getStoragePath() {
        return storagePath;
    }
    
    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }
    
    public String getStorageUrl() {
        return storageUrl;
    }
    
    public void setStorageUrl(String storageUrl) {
        this.storageUrl = storageUrl;
    }
    
    public String getMimeType() {
        return mimeType;
    }
    
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
    
    public String getUploadedBy() {
        return uploadedBy;
    }
    
    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }
    
    public String getUploadStatus() {
        return uploadStatus;
    }
    
    public void setUploadStatus(String uploadStatus) {
        this.uploadStatus = uploadStatus;
    }
    
    public Integer getPageCount() {
        return pageCount;
    }
    
    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public java.util.List<FormFieldDTO> getFormFields() {
        return formFields;
    }
    
    public void setFormFields(java.util.List<FormFieldDTO> formFields) {
        this.formFields = formFields;
    }
    
    public java.util.List<ICMPDTO> getIcmpObjects() {
        return icmpObjects;
    }
    
    public void setIcmpObjects(java.util.List<ICMPDTO> icmpObjects) {
        this.icmpObjects = icmpObjects;
    }
}

