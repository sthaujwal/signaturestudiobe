package com.wellsfargo.signaturestudio.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ICMPDTO {
    private String id;
    private String documentId;
    private String icmpType;
    private String systemOfRecord;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ICMPAttributeDTO> attributes;
    private List<ICMPRelationshipDTO> relationships;
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getDocumentId() {
        return documentId;
    }
    
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
    
    public String getIcmpType() {
        return icmpType;
    }
    
    public void setIcmpType(String icmpType) {
        this.icmpType = icmpType;
    }
    
    public String getSystemOfRecord() {
        return systemOfRecord;
    }
    
    public void setSystemOfRecord(String systemOfRecord) {
        this.systemOfRecord = systemOfRecord;
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
    
    public List<ICMPAttributeDTO> getAttributes() {
        return attributes;
    }
    
    public void setAttributes(List<ICMPAttributeDTO> attributes) {
        this.attributes = attributes;
    }
    
    public List<ICMPRelationshipDTO> getRelationships() {
        return relationships;
    }
    
    public void setRelationships(List<ICMPRelationshipDTO> relationships) {
        this.relationships = relationships;
    }
}

