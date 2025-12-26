package com.wellsfargo.signaturestudio.domain;

import java.time.Instant;
import java.util.List;

public class ICMP {
    private String id;
    private String documentId;
    private String icmpType;
    private String systemOfRecord;
    private Instant createdAt;
    private Instant updatedAt;
    private List<ICMPAttribute> attributes;
    private List<ICMPRelationship> relationships;
    
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
    
    public List<ICMPAttribute> getAttributes() {
        return attributes;
    }
    
    public void setAttributes(List<ICMPAttribute> attributes) {
        this.attributes = attributes;
    }
    
    public List<ICMPRelationship> getRelationships() {
        return relationships;
    }
    
    public void setRelationships(List<ICMPRelationship> relationships) {
        this.relationships = relationships;
    }
}

